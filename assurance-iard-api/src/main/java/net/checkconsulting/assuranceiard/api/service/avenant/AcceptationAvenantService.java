package net.checkconsulting.assuranceiard.api.service.avenant;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.dto.AcceptationAvenantRequest;
import net.checkconsulting.assuranceiard.api.dto.AvenantAccepteResponse;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.exception.AvenantInconnuException;
import net.checkconsulting.assuranceiard.api.exception.AvenantInvalideException;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Avenant;
import net.checkconsulting.assuranceiard.api.model.AvenantLigne;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.ContratOption;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.AvenantLigneRepository;
import net.checkconsulting.assuranceiard.api.repository.AvenantRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratOptionRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.service.DocumentService;
import net.checkconsulting.assuranceiard.api.service.EnvoiAvenant;
import net.checkconsulting.assuranceiard.api.service.document.DemandeDocument;
import net.checkconsulting.assuranceiard.api.service.document.DocumentsGeneresService;
import net.checkconsulting.assuranceiard.api.service.document.MotifRegeneration;
import net.checkconsulting.assuranceiard.api.service.document.RegenerationContratService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * US-23, AC4 et AC7 : acceptation de l'avenant en attente.
 * <ol>
 *   <li>Dans une transaction : acceptation horodatée et conservée (date, heure, adresse IP), date d'effet = lendemain
 *       de l'acceptation, garanties ajoutées (début à la date d'effet) ou retirées (fin la veille), nouvelle
 *       cotisation, contrat de nouveau « Actif », PDF de l'avenant (modèle {@code ModeleAvenant}).</li>
 *   <li>Après l'enregistrement complet (commentaire Jira : jamais au milieu) : nouvelle version des conditions
 *       particulières par la régénération d'US-21, motif AVENANT, si {@code estRegenerable} le permet ; puis l'avenant
 *       par e-mail.</li>
 * </ol>
 * Si un PDF ne peut pas être produit, l'acceptation reste enregistrée : la reprise de T-03 le produira.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcceptationAvenantService {

    static final String ACCEPTE = AvenantRepository.ACCEPTE;
    static final String MOTIF_DOCUMENT = "Avenant accepté";
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");

    private final AvenantService avenantService;
    private final ContratRepository contrats;
    private final AvenantRepository avenants;
    private final AvenantLigneRepository lignes;
    private final ContratOptionRepository options;
    private final CatalogueGaranties catalogue;
    private final HistoriqueAvenants historique;
    private final DocumentsGeneresService documentsGeneres;
    private final RegenerationContratService regeneration;
    private final EnvoiAvenant envoi;
    private final TransactionOperations transaction;
    private final Clock horloge;

    /** Ce que l'acceptation a enregistré, et à qui envoyer l'avenant (lu dans la même transaction). */
    private record Acceptation(Avenant avenant, List<AvenantLigne> lignes, Document document, long contratId,
            String statutContrat, String numeroContrat, String email, String prenom) {
    }

    /** AC4 : la case « J'ai lu et j'accepte mon avenant » doit être cochée (RG1 : jamais pré-cochée à l'écran). */
    @LogMetier(champs = {"resultat.disponible", "resultat.versionConditionsParticulieres"})
    public AvenantAccepteResponse accepter(String numero, AcceptationAvenantRequest demande, String adresseIp) {
        if (demande == null || !demande.accepte()) {
            throw new AvenantInvalideException(AvenantInvalideException.ACCEPTATION_REQUISE);
        }
        Acceptation acceptation = transaction.execute(etat -> enregistrer(numero, adresseIp));
        Integer version = regenererLesConditionsParticulieres(acceptation);
        boolean disponible = Boolean.TRUE.equals(acceptation.document().getStocke());
        boolean emailEnvoye = disponible && envoyer(acceptation);
        return new AvenantAccepteResponse(historique.decrire(acceptation.avenant(), acceptation.lignes()), disponible,
                emailEnvoye, version);
    }

    private Acceptation enregistrer(String numero, String adresseIp) {
        Contrat contrat = avenantService.contratModifiable(numero);
        Avenant avenant = avenants.enAttenteVerrouille(contrat.getId()).orElseThrow(AvenantInconnuException::new);
        Instant maintenant = horloge.instant();
        LocalDate effet = LocalDate.ofInstant(maintenant, FRANCE).plusDays(1);
        List<AvenantLigne> sesLignes = lignes.findByAvenantIdOrderById(avenant.getId());

        enregistrerLesOptionsDuDevis(contrat, avenantService.donnees(contrat));
        sesLignes.forEach(ligne -> appliquer(contrat.getId(), ligne, effet));
        avenant.setStatut(ACCEPTE);
        avenant.setAccepteLe(maintenant);
        avenant.setAccepteIp(adresseIp);
        avenant.setDateEffet(effet);
        avenants.saveAndFlush(avenant);

        contrat.setCotisationMensuelle(avenant.getPrimeApresTtc());
        contrat.setCotisationAnnuelle(CalculAvenant.annuelle(avenant.getPrimeApresTtc()));
        contrats.save(contrat);
        avenantService.revenirAActif(contrat, "Avenant accepté en ligne");

        Document document = documentsGeneres.generer(new DemandeDocument(TypeDocument.AVENANT, contrat.getId(),
                NumeroAvenant.de(avenant), MOTIF_DOCUMENT));
        avenant.setDocumentId(document.getId());
        avenants.save(avenant);
        log.info("Avenant accepté : contrat={}, avenant={}, stocke={}", contrat.getId(), avenant.getId(),
                document.getStocke());
        Compte compte = contrat.getCompte();
        return new Acceptation(avenant, sesLignes, document, contrat.getId(), contrat.getStatut(),
                contrat.getNumero(), compte.getEmail(), compte.getPrenom());
    }

    /**
     * Contrat du sprint 1 sans ligne contrat_option : ses options (celles du devis) sont d'abord enregistrées, au prix
     * de la grille, pour que l'avenant s'y applique.
     */
    private void enregistrerLesOptionsDuDevis(Contrat contrat, DonneesContrat donnees) {
        if (options.existsByContratId(contrat.getId()) || donnees.options().isEmpty()) {
            return;
        }
        catalogue.garanties(contrat, donnees, contrat.getDateEffet()).stream()
                .filter(GarantieProposee::souscrite)
                .forEach(garantie -> options.save(option(contrat.getId(), garantie.code(),
                        garantie.prixMensuelTtc() == null ? BigDecimal.ZERO : garantie.prixMensuelTtc(),
                        garantie.obligatoire(), contrat.getDateEffet())));
    }

    /** Ajout : la garantie commence à la date d'effet. Retrait : elle se termine la veille de la date d'effet. */
    private void appliquer(long contratId, AvenantLigne ligne, LocalDate effet) {
        if (CalculAvenant.AJOUT.equals(ligne.getAction())) {
            options.save(option(contratId, ligne.getCodeOption(), ligne.getPrixMensuelTtc(), false, effet));
            return;
        }
        options.enCours(contratId, effet).stream()
                .filter(option -> option.getCodeOption().equals(ligne.getCodeOption()))
                .forEach(option -> option.setDateFin(effet.minusDays(1)));
    }

    private static ContratOption option(long contratId, String code, BigDecimal prix, boolean obligatoire,
            LocalDate debut) {
        ContratOption option = new ContratOption();
        option.setContratId(contratId);
        option.setCodeOption(code);
        option.setPrixMensuelTtc(prix);
        option.setObligatoire(obligatoire);
        option.setDateDebut(debut);
        return option;
    }

    /** AC7 : régénération d'US-21 (motif AVENANT), jamais pour un contrat qui n'est plus en vigueur. */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private Integer regenererLesConditionsParticulieres(Acceptation acceptation) {
        if (!RegenerationContratService.estRegenerable(acceptation.statutContrat(), MotifRegeneration.AVENANT)) {
            return null;
        }
        try {
            return regeneration.regenerer(acceptation.contratId(), MotifRegeneration.AVENANT).version();
        } catch (RuntimeException erreur) {
            log.warn("Conditions particulières non régénérées après l'avenant : contrat={} ({})",
                    acceptation.contratId(), erreur.getClass().getSimpleName());
            return null;
        }
    }

    /** AC4 : l'avenant par e-mail ; un échec d'envoi ne remet pas en cause l'acceptation. */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private boolean envoyer(Acceptation acceptation) {
        try {
            DocumentService.Fichier fichier = documentsGeneres.telecharger(acceptation.document().getId());
            envoi.envoyer(acceptation.email(), acceptation.prenom(), acceptation.numeroContrat(),
                    NumeroAvenant.de(acceptation.avenant()), fichier.nom(), fichier.contenu());
            return true;
        } catch (RuntimeException erreur) {
            log.warn("E-mail de l'avenant non envoyé : avenant={} ({})", acceptation.avenant().getId(),
                    erreur.getClass().getSimpleName());
            return false;
        }
    }
}
