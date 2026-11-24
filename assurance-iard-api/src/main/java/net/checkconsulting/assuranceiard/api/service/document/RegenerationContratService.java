package net.checkconsulting.assuranceiard.api.service.document;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.dto.RegenerationResponse;
import net.checkconsulting.assuranceiard.api.exception.ContratInconnuException;
import net.checkconsulting.assuranceiard.api.exception.RegenerationImpossibleException;
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
 * EN_VIGUEUR, version N REMPLACE, PDF stocké avec son empreinte), puis les envoie par e-mail (AC3).
 * RG1 : ne modifie ni les garanties ni la cotisation (seul un document est créé). RG2 : impossible sur un contrat
 * Suspendu, et plus généralement dans les statuts où RG3 d'US-18 bloque la régénération. Une régénération par
 * minute et par contrat. Le contrôle d'accès (propriétaire du contrat) est fait avant, par la route (US-04).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegenerationContratService {

    /** RG3 d'US-18 (statut-contrat.ts) : statuts qui permettent de régénérer le contrat. */
    public static final Set<String> STATUTS_PERMIS = Set.of("ACTIF", "ACTIF_JUSTIFICATIFS_ATTENDUS",
            "AVENANT_EN_ATTENTE");
    static final Duration DELAI_MINIMUM = Duration.ofMinutes(1);
    static final String MOTIF = "Régénération à la demande du client";
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

    @LogMetier(champs = {"resultat.version", "resultat.disponible"})
    public RegenerationResponse regenerer(String numero) {
        Generation generation;
        try {
            generation = transaction.execute(etat -> generer(numero));
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

    private Generation generer(String numero) {
        Contrat contrat = contrats.parNumero(numero).orElseThrow(ContratInconnuException::new);
        if (!STATUTS_PERMIS.contains(contrat.getStatut())) {
            throw new RegenerationImpossibleException();
        }
        // Verrou sur la version en vigueur : deux demandes simultanées ne se croisent pas. Seule une régénération
        // récente bloque (une V1 créée à la signature ou par les données de test ne compte pas).
        documents.enVigueurVerrouille(contrat.getId(), CONDITIONS_PARTICULIERES, "")
                .filter(actuelle -> MOTIF.equals(actuelle.getMotif()))
                .map(Document::getDateCreation)
                .filter(this::tropRecente)
                .ifPresent(date -> {
                    throw new RegenerationTropRapprocheeException();
                });
        Document document = service.generer(
                DemandeDocument.duContrat(TypeDocument.CONDITIONS_PARTICULIERES, contrat.getId(), MOTIF));
        Compte compte = contrat.getCompte();
        return new Generation(document, contrat.getNumero(), compte.getEmail(), compte.getPrenom());
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
