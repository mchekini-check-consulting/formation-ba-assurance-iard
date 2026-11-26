package net.checkconsulting.assuranceiard.api.service.avenant;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.dto.AvenantResponse;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.dto.GarantiesContratResponse;
import net.checkconsulting.assuranceiard.api.dto.PreparationAvenantRequest;
import net.checkconsulting.assuranceiard.api.dto.ProjetAvenantResponse;
import net.checkconsulting.assuranceiard.api.exception.AvenantInconnuException;
import net.checkconsulting.assuranceiard.api.exception.ContratInconnuException;
import net.checkconsulting.assuranceiard.api.exception.ModificationGarantiesImpossibleException;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Avenant;
import net.checkconsulting.assuranceiard.api.model.AvenantLigne;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.HistoriqueStatutContrat;
import net.checkconsulting.assuranceiard.api.repository.AvenantLigneRepository;
import net.checkconsulting.assuranceiard.api.repository.AvenantRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.repository.HistoriqueStatutContratRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-23 : « Modifier mes garanties » d'un contrat habitation ou auto (AC1, AC2, AC3, AC5, AC6).
 * <ul>
 *   <li>AC6 : seulement pour un contrat Actif (ou Actif avec un avenant en attente, pour le remplacer) ; le serveur
 *       refuse aussi l'appel direct (422).</li>
 *   <li>Un seul avenant en attente par contrat : un nouvel avenant préparé remplace l'ancien, qui passe à ABANDONNE.
 *       Rien n'est effacé.</li>
 *   <li>AC5 : tant qu'il n'est pas accepté, le contrat est « Avenant en attente » et reste couvert aux conditions
 *       actuelles (garanties et cotisation inchangées).</li>
 * </ul>
 * L'acceptation (AC4, AC7) est faite par {@link AcceptationAvenantService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvenantService {

    public static final String ACTIF = "ACTIF";
    public static final String AVENANT_EN_ATTENTE = "AVENANT_EN_ATTENTE";
    /** AC6 (décision BA, à valider PO) : seul un contrat actif se modifie ; « Avenant en attente » est un contrat actif. */
    public static final Set<String> STATUTS_MODIFIABLES = Set.of(ACTIF, AVENANT_EN_ATTENTE);
    static final String ABANDONNE = "ABANDONNE";
    static final String REMPLACE = "REMPLACE";
    static final String AUTEUR_CLIENT = "CLIENT";
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");

    private final ContratRepository contrats;
    private final DonneesContratRepository donneesContrat;
    private final AvenantRepository avenants;
    private final AvenantLigneRepository lignes;
    private final DocumentRepository documents;
    private final HistoriqueStatutContratRepository historique;
    private final CatalogueGaranties catalogue;
    private final HistoriqueAvenants historiqueAvenants;
    private final ProjetsAvenant projets;
    private final Clock horloge;

    /** AC1, AC2, AC5 : garanties actuelles et ajoutables, avec leur prix, et l'avenant en attente s'il y en a un. */
    @LogMetier(champs = {"resultat.produit", "resultat.statut"})
    @Transactional(readOnly = true)
    public GarantiesContratResponse garanties(String numero) {
        Contrat contrat = contratModifiable(numero);
        DonneesContrat donnees = donnees(contrat);
        List<GarantiesContratResponse.Garantie> garanties = catalogue.garanties(contrat, donnees, aujourdhui()).stream()
                .map(garantie -> new GarantiesContratResponse.Garantie(garantie.code(), garantie.libelle(),
                        garantie.prixMensuelTtc(), garantie.souscrite(), garantie.obligatoire(),
                        garantie.modifiable(), garantie.raison()))
                .toList();
        ProjetAvenantResponse enAttente = historiqueAvenants.enAttente(contrat.getId())
                .map(avenant -> projets.projet(contrat, donnees, avenant))
                .orElse(null);
        return new GarantiesContratResponse(contrat.getNumero(), contrat.getProduit(), contrat.getFormule(),
                contrat.getStatut(), contrat.getCompte().getPrenom(), contrat.getCotisationMensuelle(),
                contrat.getCotisationAnnuelle(), donnees.paiement().frequence(), garanties, enAttente);
    }

    /**
     * AC3 : prépare l'avenant (garanties voulues après l'avenant) et renvoie ce qui change. Le contrat passe à
     * « Avenant en attente » ; un avenant déjà en attente est remplacé.
     */
    @LogMetier(champs = {"resultat.reductionCouverture"})
    @Transactional
    public ProjetAvenantResponse preparer(String numero, PreparationAvenantRequest demande) {
        Contrat contrat = contratModifiable(numero);
        DonneesContrat donnees = donnees(contrat);
        CalculAvenant.Changement changement = CalculAvenant.calculer(
                catalogue.garanties(contrat, donnees, aujourdhui()), demande.garanties(),
                contrat.getCotisationMensuelle());

        avenants.enAttenteVerrouille(contrat.getId()).ifPresent(this::passerAbandonne);
        Avenant avenant = new Avenant();
        avenant.setNumero(avenants.prochainNumero(contrat.getId()));
        avenant.setContratId(contrat.getId());
        avenant.setStatut(AvenantRepository.EN_ATTENTE);
        avenant.setDateEffet(historiqueAvenants.lendemain());
        avenant.setPrimeAvantTtc(changement.primeAvant());
        avenant.setPrimeApresTtc(changement.primeApres());
        avenants.saveAndFlush(avenant);
        List<AvenantLigne> sesLignes = changement.lignes().stream().map(ligne -> ligne(avenant, ligne)).toList();
        lignes.saveAll(sesLignes);
        changerStatut(contrat, AVENANT_EN_ATTENTE, "Avenant préparé en ligne");
        log.info("Avenant préparé : contrat={}, avenant={}, ajouts={}, retraits={}", contrat.getId(), avenant.getId(),
                changement.ajouts().size(), changement.retraits().size());
        return projets.projet(contrat, donnees, historiqueAvenants.decrire(avenant, sesLignes));
    }

    /** AC5 : abandonne l'avenant en attente (ABANDONNE, rien n'est effacé) ; le contrat redevient Actif. */
    @LogMetier
    @Transactional
    public AvenantResponse abandonner(String numero) {
        Contrat contrat = contrats.parNumero(numero).orElseThrow(ContratInconnuException::new);
        Avenant avenant = avenants.enAttenteVerrouille(contrat.getId()).orElseThrow(AvenantInconnuException::new);
        passerAbandonne(avenant);
        revenirAActif(contrat, "Avenant abandonné par le client");
        log.info("Avenant abandonné : contrat={}, avenant={}", contrat.getId(), avenant.getId());
        return historiqueAvenants.decrire(avenant, lignes.findByAvenantIdOrderById(avenant.getId()));
    }

    /** AC6 : contrat actif, sinon 422 (même si l'écran ne propose pas l'action). */
    Contrat contratModifiable(String numero) {
        Contrat contrat = contrats.parNumero(numero).orElseThrow(ContratInconnuException::new);
        if (!STATUTS_MODIFIABLES.contains(contrat.getStatut())) {
            throw new ModificationGarantiesImpossibleException();
        }
        return contrat;
    }

    DonneesContrat donnees(Contrat contrat) {
        return donneesContrat.lire(contrat.getId()).orElseThrow(ContratInconnuException::new);
    }

    /** « Avenant en attente » redevient « Actif » ; tout autre statut (ex. Impayé entre-temps) est gardé. */
    void revenirAActif(Contrat contrat, String motif) {
        if (AVENANT_EN_ATTENTE.equals(contrat.getStatut())) {
            changerStatut(contrat, ACTIF, motif);
        }
    }

    /** Enregistre le changement de statut du contrat et son historique (rien si le statut ne change pas). */
    void changerStatut(Contrat contrat, String statut, String motif) {
        String avant = contrat.getStatut();
        if (statut.equals(avant)) {
            return;
        }
        contrat.setStatut(statut);
        contrats.save(contrat);
        HistoriqueStatutContrat ligne = new HistoriqueStatutContrat();
        ligne.setContratId(contrat.getId());
        ligne.setStatutAvant(avant);
        ligne.setStatutApres(statut);
        ligne.setChangeLe(Instant.now(horloge));
        ligne.setMotif(motif);
        ligne.setAuteur(AUTEUR_CLIENT);
        historique.save(ligne);
    }

    /**
     * L'avenant passe à ABANDONNE (rien n'est effacé) ; un projet d'avenant déjà imprimé (données de test T-05) est
     * marqué « remplacé » : il n'apparaît plus parmi les documents en vigueur.
     */
    private void passerAbandonne(Avenant avenant) {
        avenant.setStatut(ABANDONNE);
        // Écrit avant tout nouvel avenant : un seul EN_ATTENTE par contrat (index unique).
        avenants.saveAndFlush(avenant);
        if (avenant.getDocumentId() != null) {
            documents.findById(avenant.getDocumentId()).ifPresent(document -> document.setStatut(REMPLACE));
        }
    }

    private static AvenantLigne ligne(Avenant avenant, CalculAvenant.Ligne changement) {
        AvenantLigne ligne = new AvenantLigne();
        ligne.setAvenantId(avenant.getId());
        ligne.setCodeOption(changement.code());
        ligne.setAction(changement.action());
        ligne.setPrixMensuelTtc(changement.prixMensuelTtc());
        return ligne;
    }

    private LocalDate aujourdhui() {
        return LocalDate.ofInstant(horloge.instant(), FRANCE);
    }
}
