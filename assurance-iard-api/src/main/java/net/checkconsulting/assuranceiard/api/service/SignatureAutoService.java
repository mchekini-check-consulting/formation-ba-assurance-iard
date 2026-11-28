package net.checkconsulting.assuranceiard.api.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.config.DocumentsProperties;
import net.checkconsulting.assuranceiard.api.dto.ContratAutoResponse;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.dto.EnvoyerCodeSignatureResponse;
import net.checkconsulting.assuranceiard.api.dto.SignatureContratRequest;
import net.checkconsulting.assuranceiard.api.exception.AccesNonAutoriseException;
import net.checkconsulting.assuranceiard.api.exception.CodeSignatureEpuiseException;
import net.checkconsulting.assuranceiard.api.exception.CodeSignatureIncorrectException;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.DevisAuto;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.HistoriqueStatutContrat;
import net.checkconsulting.assuranceiard.api.model.PreuveSignature;
import net.checkconsulting.assuranceiard.api.model.Profil;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisAutoRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentContenuRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.repository.HistoriqueStatutContratRepository;
import net.checkconsulting.assuranceiard.api.repository.PreuveSignatureRepository;
import net.checkconsulting.assuranceiard.api.service.document.ConditionsParticulieresALaSignature;
import net.checkconsulting.assuranceiard.api.service.document.GabaritPdf;
import net.checkconsulting.assuranceiard.api.service.document.MemoVehiculeALaSignature;
import net.checkconsulting.assuranceiard.api.service.document.ModeleConditionsParticulieresAuto;
import net.checkconsulting.assuranceiard.api.service.fva.FvaService;
import net.checkconsulting.assuranceiard.api.service.hamon.HamonService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** US-33 : lecture, envoi du code de signature et signature d'un contrat auto. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignatureAutoService {

    private static final String EN_ATTENTE = "EN_ATTENTE_DE_SIGNATURE";
    private static final String EN_ATTENTE_EFFET = "EN_ATTENTE_EFFET";
    private static final String ACTIF_JUSTIFICATIFS = "ACTIF_JUSTIFICATIFS_ATTENDUS";
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");

    private static final List<String> JUSTIFICATIFS_AUTO = List.of(
            "Permis de conduire du conducteur principal",
            "Carte grise du véhicule assuré");
    private static final String JUSTIFICATIF_RIB = "RIB récent du titulaire du compte bancaire";

    private final SessionService sessionService;
    private final ContratRepository contratsRepo;
    private final DevisAutoRepository devisAutos;
    private final DonneesContratRepository donneesContrats;
    private final DocumentRepository documents;
    private final DocumentContenuRepository documentContenus;
    private final PreuveSignatureRepository preuves;
    private final HistoriqueStatutContratRepository historique;
    private final CodeSignatureContratService codeSignatureService;
    private final ConditionsParticulieresALaSignature conditionsParticulieres;
    private final MemoVehiculeALaSignature memoVehicule;
    private final ModeleConditionsParticulieresAuto modeleCpAuto;
    private final EnvoiEmailContrat emailService;
    private final FvaService fvaService;
    private final HamonService hamonService;
    private final DocumentsProperties documentsProps;
    private final Clock horloge;

    /** Charge les données du contrat pour l'écran de signature (GET étape 6). */
    @Transactional(readOnly = true)
    @LogMetier(champs = {"resultat.statut"})
    public ContratAutoResponse obtenir(Long devisAutoId, String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        Contrat contrat = chargerEtVerifier(devisAutoId, compte);
        return construireReponse(contrat);
    }

    /** Envoie le code de signature par e-mail. */
    @Transactional
    @LogMetier
    public EnvoyerCodeSignatureResponse envoyerCode(Long devisAutoId, String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        Contrat contrat = chargerEtVerifier(devisAutoId, compte);
        String emailMasque = codeSignatureService.envoyer(contrat);
        return new EnvoyerCodeSignatureResponse(emailMasque);
    }

    /** Renvoie un nouveau code de signature (invalide le précédent). */
    @Transactional
    @LogMetier
    public EnvoyerCodeSignatureResponse renvoyerCode(Long devisAutoId, String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        Contrat contrat = chargerEtVerifier(devisAutoId, compte);
        String emailMasque = codeSignatureService.renvoyer(contrat);
        return new EnvoyerCodeSignatureResponse(emailMasque);
    }

    /**
     * Signe le contrat après vérification du code.
     * Transition : EN_ATTENTE_DE_SIGNATURE → EN_ATTENTE_EFFET ou ACTIF_JUSTIFICATIFS_ATTENDUS.
     */
    @Transactional(noRollbackFor = {CodeSignatureIncorrectException.class, CodeSignatureEpuiseException.class})
    @LogMetier(champs = {"resultat.statut"})
    public ContratAutoResponse signer(Long devisAutoId, SignatureContratRequest demande, String adresseIp,
            String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        Contrat contrat = chargerEtVerifier(devisAutoId, compte);

        codeSignatureService.verifier(contrat, demande.code());

        // Calcul du nouveau statut
        LocalDate aujourd_hui = LocalDate.now(horloge.withZone(FRANCE));
        boolean immediat = !contrat.getDateEffet().isAfter(aujourd_hui);
        String statutAvant = contrat.getStatut();
        String nouveauStatut = immediat ? ACTIF_JUSTIFICATIFS : EN_ATTENTE_EFFET;

        contrat.setStatut(nouveauStatut);
        contrat.setSigneLe(horloge.instant());
        contrat = contratsRepo.save(contrat);

        // Historique
        HistoriqueStatutContrat ligne = new HistoriqueStatutContrat();
        ligne.setContratId(contrat.getId());
        ligne.setStatutAvant(statutAvant);
        ligne.setStatutApres(nouveauStatut);
        ligne.setChangeLe(horloge.instant());
        ligne.setMotif("Signature électronique");
        ligne.setAuteur("CLIENT");
        historique.save(ligne);

        // Profil PROSPECT → ASSURE
        if (compte.getProfil() == Profil.PROSPECT) {
            compte.setProfil(Profil.ASSURE);
            log.info("layer=service Profil mis à jour après signature auto : profil={}", Profil.ASSURE);
        }

        // Empreinte du CP V1 (calculée depuis le contenu, avant création du document)
        Document tempDoc = new Document();
        tempDoc.setType(TypeDocument.CONDITIONS_PARTICULIERES.name());
        tempDoc.setContratId(contrat.getId());
        tempDoc.setReference("");
        tempDoc.setVersion(1);
        String empreinteCP = GabaritPdf.empreinte(modeleCpAuto.rediger(tempDoc));

        // Preuve de signature
        PreuveSignature preuve = new PreuveSignature();
        preuve.setContratId(contrat.getId());
        preuve.setEmpreinteDocument(empreinteCP);
        preuve.setEmail(compte.getEmail());
        preuve.setAdresseIp(adresseIp);
        preuve.setSigneLe(horloge.instant());
        preuve.setNbEssais(1);
        preuves.save(preuve);

        log.info("layer=service Contrat auto signé : contrat={}, statut={}", contrat.getNumero(), nouveauStatut);

        // Données pour les callbacks after-commit
        final long contratId = contrat.getId();
        final String email = compte.getEmail();
        final String prenom = compte.getPrenom();
        final String nom = compte.getNom();
        final String numero = contrat.getNumero();
        final boolean hamonApplicable = Boolean.TRUE.equals(contrat.getResiliationHamon());
        final String immatriculation = vehiculeImmatriculation(contrat);
        final LocalDate dateEffet = contrat.getDateEffet();

        // Enregistrer les CP et mémo (doivent être créés avant l'email)
        conditionsParticulieres.creerApresSignature(contratId);
        memoVehicule.creerApresSignature(contratId);

        // After-commit : FVA, Hamon, email (après CP et mémo)
        executerApresCommit(() -> postSignature(contratId, email, prenom, nom, numero,
                hamonApplicable, immatriculation, dateEffet, devisAutoId));

        return construireReponse(contrat);
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------

    private Contrat chargerEtVerifier(Long devisAutoId, Compte compte) {
        Contrat contrat = contratsRepo.findByDevisAutoId(devisAutoId)
                .orElseThrow(DevisInconnuException::new);
        if (contrat.getCompte() == null || !compte.getId().equals(contrat.getCompte().getId())) {
            throw new AccesNonAutoriseException();
        }
        return contrat;
    }

    private ContratAutoResponse construireReponse(Contrat contrat) {
        DonneesContrat donnees = donneesContrats.lire(contrat.getId())
                .orElseThrow(DevisInconnuException::new);

        List<Document> docs = documents.enVigueurDuContrat(contrat.getId(), List.of(
                TypeDocument.CONDITIONS_PARTICULIERES.name(),
                TypeDocument.MEMO_VEHICULE.name(),
                TypeDocument.MANDAT_SEPA.name()));

        Long cpId = idDoc(docs, TypeDocument.CONDITIONS_PARTICULIERES.name());
        Long memoId = idDoc(docs, TypeDocument.MEMO_VEHICULE.name());
        Long mandatId = idDoc(docs, TypeDocument.MANDAT_SEPA.name());

        DonneesContrat.Souscripteur souscripteur = donnees.souscripteur();
        DonneesContrat.Vehicule vehicule = donnees.vehicule();
        DonneesContrat.Conducteur conducteur = donnees.conducteur();
        DonneesContrat.Tarif tarif = donnees.tarif();
        DonneesContrat.Paiement paiement = donnees.paiement();
        DonneesContrat.AncienContrat ancien = donnees.ancienContrat();

        return new ContratAutoResponse(
                donnees.numero(),
                contrat.getStatut(),
                souscripteur.civilite(), souscripteur.prenom(), souscripteur.nom(),
                vehicule != null ? vehicule.marque() : null,
                vehicule != null ? vehicule.modele() : null,
                vehicule != null ? vehicule.version() : null,
                vehicule != null ? vehicule.immatriculation() : null,
                conducteur != null ? conducteur.coefficientBonusMalus() : null,
                nomConducteur(conducteur, souscripteur),
                donnees.formule(),
                options(donnees),
                tarif != null ? tarif.franchise() : null,
                tarif != null ? tarif.cotisationMensuelle() : null,
                tarif != null ? tarif.cotisationAnnuelle() : null,
                tarif != null ? tarif.dateEffet() : null,
                paiement != null ? paiement.jourPrelevement() : null,
                paiement != null ? paiement.ibanMasque() : null,
                paiement != null ? paiement.rum() : null,
                ancien != null ? ancien.assureur() : null,
                ancien != null ? ancien.numero() : null,
                cpId, memoId, mandatId,
                cgId(donnees), ipidId(donnees), ficheConseilId(contrat.getDevisAutoId()),
                justificatifsAuto(contrat),
                documentsProps.exclusionsDe(donnees.formule()));
    }

    private Long cgId(DonneesContrat donnees) {
        String ref = donnees.produit() != null ? donnees.produit() : "";
        return documents.enVigueur(0L, TypeDocument.CONDITIONS_GENERALES.name(), ref)
                .map(Document::getId).orElse(null);
    }

    private Long ipidId(DonneesContrat donnees) {
        String ref = donnees.formule() != null ? donnees.formule() : "";
        return documents.enVigueur(0L, TypeDocument.IPID.name(), ref)
                .map(Document::getId).orElse(null);
    }

    private Long ficheConseilId(Long devisAutoId) {
        if (devisAutoId == null) return null;
        return documents.enVigueur(0L, TypeDocument.FICHE_CONSEIL.name(), String.valueOf(devisAutoId))
                .map(Document::getId).orElse(null);
    }

    private List<String> justificatifsAuto(Contrat contrat) {
        List<String> justificatifs = new ArrayList<>(JUSTIFICATIFS_AUTO);
        if (Boolean.TRUE.equals(contrat.getResiliationHamon())) {
            justificatifs.add(JUSTIFICATIF_RIB);
        }
        return justificatifs;
    }

    private String nomConducteur(DonneesContrat.Conducteur conducteur, DonneesContrat.Souscripteur souscripteur) {
        if (conducteur == null || !Boolean.TRUE.equals(conducteur.estSouscripteur()) || souscripteur == null) {
            return null;
        }
        return souscripteur.prenom() + " " + souscripteur.nom();
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void postSignature(long contratId, String email, String prenom, String nom, String numero,
            boolean hamonApplicable, String immatriculation, LocalDate dateEffet, Long devisAutoId) {
        // FVA
        try {
            fvaService.declarer(contratId, immatriculation);
        } catch (RuntimeException e) {
            log.warn("layer=service FVA non déclaré après signature : contrat={}", contratId);
        }

        // Hamon
        if (hamonApplicable) {
            try {
                String ancienAssureur = "Assureur précédent";
                String ancienNumero = "INCONNU";
                Optional<DevisAuto> devis = devisAutos.findById(devisAutoId);
                if (devis.isPresent() && devis.get().getAncienAssureur() != null) {
                    ancienAssureur = devis.get().getAncienAssureur();
                    ancienNumero = devis.get().getAncienNumeroContrat() != null
                            ? devis.get().getAncienNumeroContrat() : ancienNumero;
                }
                hamonService.resilier(contratId, ancienAssureur, ancienNumero, dateEffet);
            } catch (RuntimeException e) {
                log.warn("layer=service Hamon non envoyé après signature : contrat={}", contratId);
            }
        }

        // Email de confirmation (après CP et mémo créés)
        try {
            List<Document> docs = documents.enVigueurDuContrat(contratId, List.of(
                    TypeDocument.CONDITIONS_PARTICULIERES.name(),
                    TypeDocument.MEMO_VEHICULE.name()));
            byte[] pdfCp = pdfBytes(idDoc(docs, TypeDocument.CONDITIONS_PARTICULIERES.name()));
            byte[] pdfMemo = pdfBytes(idDoc(docs, TypeDocument.MEMO_VEHICULE.name()));
            emailService.envoyerConfirmationSignature(email, prenom, nom, numero, pdfCp, pdfMemo);
        } catch (RuntimeException e) {
            log.warn("layer=service Email confirmation non envoyé : contrat={}", contratId);
        }
    }

    private byte[] pdfBytes(Long docId) {
        if (docId == null) return null;
        return documentContenus.findByDocumentId(docId)
                .map(dc -> dc.getContenu())
                .orElse(null);
    }

    private static Long idDoc(List<Document> docs, String type) {
        return docs.stream().filter(d -> type.equals(d.getType())).map(Document::getId).findFirst().orElse(null);
    }

    private static List<String> options(DonneesContrat donnees) {
        if (donnees.options() == null) return List.of();
        return donnees.options().stream().map(DonneesContrat.Option::code).toList();
    }

    private String vehiculeImmatriculation(Contrat contrat) {
        // L'immatriculation est disponible dans DonneesContrat mais ça ré-exécute une requête.
        // Pour performance, on la récupère ici via le véhicule déjà chargé en lazy.
        try {
            return contrat.getVehicule() != null ? contrat.getVehicule().getImmatriculation() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void executerApresCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
