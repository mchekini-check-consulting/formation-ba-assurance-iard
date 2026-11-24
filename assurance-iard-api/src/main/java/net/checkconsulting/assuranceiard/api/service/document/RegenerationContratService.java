package net.checkconsulting.assuranceiard.api.service.document;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.dto.RegenerationResponse;
import net.checkconsulting.assuranceiard.api.exception.ContratInconnuException;
import net.checkconsulting.assuranceiard.api.exception.RegenerationImpossibleException;
import net.checkconsulting.assuranceiard.api.exception.RegenerationInutileException;
import net.checkconsulting.assuranceiard.api.exception.RegenerationTropRapprocheeException;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.service.DocumentService;
import net.checkconsulting.assuranceiard.api.service.EnvoiConditionsParticulieres;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * US-21 : régénère les conditions particulières d'un contrat avec le service de documents de T-03 (version N+1
 * EN_VIGUEUR, version N REMPLACE, PDF stocké avec son empreinte), puis les envoie par e-mail (AC3). Le motif est
 * enregistré dans document.motif ({@link MotifRegeneration}).
 *
 * <p>Point d'entrée unique, réutilisable par les autres US : {@link #regenerer(long, MotifRegeneration)}. Seule la
 * demande du client (DEMANDE_CLIENT) est soumise à ses règles : RG2 / RG3 d'US-18 (statut), une par minute, et
 * « données inchangées ». Un déclenchement automatique (AVENANT, COORDONNEES, CONDITIONS_GENERALES) n'est jamais
 * bloqué. RG1 : ni les garanties ni la cotisation ne changent (seul un document est créé).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegenerationContratService {

    /** RG3 d'US-18 (statut-contrat.ts) : statuts qui permettent au client de régénérer son contrat. */
    public static final Set<String> STATUTS_PERMIS = Set.of("ACTIF", "ACTIF_JUSTIFICATIFS_ATTENDUS",
            "AVENANT_EN_ATTENTE");
    static final Duration DELAI_MINIMUM = Duration.ofMinutes(1);
    private static final String CONDITIONS_PARTICULIERES = TypeDocument.CONDITIONS_PARTICULIERES.name();

    private final ContratRepository contrats;
    private final DocumentRepository documents;
    private final DocumentsGeneresService service;
    private final EnvoiConditionsParticulieres envoi;
    private final TransactionOperations transaction;
    private final Clock horloge;

    /** Ce que la génération a produit, et à qui l'envoyer (lu dans la même transaction). */
    private record Generation(Document document, String numero, String email, String prenom) {
    }

    /** Bouton du client (route POST /contrats/{numero}/conditions-particulieres). */
    @LogMetier(champs = {"resultat.version", "resultat.disponible"})
    public RegenerationResponse regenererALaDemandeDuClient(String numero) {
        long contratId = contrats.parNumero(numero).map(Contrat::getId).orElseThrow(ContratInconnuException::new);
        return regenerer(contratId, MotifRegeneration.DEMANDE_CLIENT);
    }

    /**
     * Méthode réutilisable : régénère les conditions particulières du contrat pour ce motif, puis les envoie par
     * e-mail. Si elle est appelée dans une transaction en cours, elle s'y joint.
     */
    @LogMetier(champs = {"motif", "resultat.version", "resultat.disponible"})
    public RegenerationResponse regenerer(long contratId, MotifRegeneration motif) {
        Generation generation;
        try {
            generation = transaction.execute(etat -> generer(contratId, motif));
        } catch (DataIntegrityViolationException demandeSimultanee) {
            // Deux demandes en même temps : la seconde se heurte à la version créée par la première.
            throw new RegenerationTropRapprocheeException();
        }
        Document document = generation.document();
        boolean disponible = Boolean.TRUE.equals(document.getStocke());
        boolean emailEnvoye = disponible && envoyer(generation);
        return new RegenerationResponse(document.getId(), document.getVersion(), document.getDateCreation(),
                disponible, emailEnvoye);
    }

    private Generation generer(long contratId, MotifRegeneration motif) {
        Contrat contrat = contrats.findById(contratId).orElseThrow(ContratInconnuException::new);
        // Verrou sur la version en vigueur : deux demandes simultanées ne se croisent pas.
        Optional<Document> actuelle = documents.enVigueurVerrouille(contratId, CONDITIONS_PARTICULIERES, "");
        if (motif.demandeDuClient()) {
            verifierLaDemandeDuClient(contrat, actuelle);
        }
        Document document = service.generer(
                DemandeDocument.duContrat(TypeDocument.CONDITIONS_PARTICULIERES, contratId, motif.name()));
        Compte compte = contrat.getCompte();
        return new Generation(document, contrat.getNumero(), compte.getEmail(), compte.getPrenom());
    }

    /** Règles du bouton : statut (RG2, RG3 d'US-18), une régénération par minute, données changées. */
    private void verifierLaDemandeDuClient(Contrat contrat, Optional<Document> actuelle) {
        if (!STATUTS_PERMIS.contains(contrat.getStatut())) {
            throw new RegenerationImpossibleException();
        }
        if (actuelle.isEmpty()) {
            return;
        }
        Document enVigueur = actuelle.get();
        if (MotifRegeneration.DEMANDE_CLIENT.name().equals(enVigueur.getMotif())
                && tropRecente(enVigueur.getDateCreation())) {
            throw new RegenerationTropRapprocheeException();
        }
        if (service.contenuAJour(enVigueur)) {
            throw new RegenerationInutileException(enVigueur.getVersion());
        }
    }

    private boolean tropRecente(Instant creation) {
        return creation != null && creation.isAfter(horloge.instant().minus(DELAI_MINIMUM));
    }

    /** AC3 : un échec d'envoi ne remet pas en cause la nouvelle version (déjà enregistrée). */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private boolean envoyer(Generation generation) {
        Document document = generation.document();
        try {
            DocumentService.Fichier fichier = service.telecharger(document.getId());
            envoi.envoyer(generation.email(), generation.prenom(), generation.numero(), document.getVersion(),
                    fichier.nom(), fichier.contenu());
            return true;
        } catch (RuntimeException erreur) {
            log.warn("E-mail des conditions particulières non envoyé : document={}, version={} ({})",
                    document.getId(), document.getVersion(), erreur.getClass().getSimpleName());
            return false;
        }
    }
}
