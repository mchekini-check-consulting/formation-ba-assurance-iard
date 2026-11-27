package net.checkconsulting.assuranceiard.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.dto.ContratAutoGenerationResponse;
import net.checkconsulting.assuranceiard.api.dto.IbanVerificationRequest;
import net.checkconsulting.assuranceiard.api.dto.IbanVerificationResponse;
import net.checkconsulting.assuranceiard.api.dto.PaiementAutoInitResponse;
import net.checkconsulting.assuranceiard.api.dto.PaiementAutoInitResponse.OptionPrix;
import net.checkconsulting.assuranceiard.api.dto.PaiementAutoRequest;
import net.checkconsulting.assuranceiard.api.exception.InformationRefuseeException;
import net.checkconsulting.assuranceiard.api.exception.VehiculeIntrouvableException;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Conducteur;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.DevisAuto;
import net.checkconsulting.assuranceiard.api.model.DocumentContenu;
import net.checkconsulting.assuranceiard.api.model.Echeance;
import net.checkconsulting.assuranceiard.api.model.MandatSepa;
import net.checkconsulting.assuranceiard.api.model.SouscripteurAuto;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.model.Vehicule;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.repository.ConducteurRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisAutoRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentContenuRepository;
import net.checkconsulting.assuranceiard.api.repository.EcheanceRepository;
import net.checkconsulting.assuranceiard.api.repository.MandatSepaRepository;
import net.checkconsulting.assuranceiard.api.repository.SouscripteurAutoRepository;
import net.checkconsulting.assuranceiard.api.repository.VehiculeRepository;
import net.checkconsulting.assuranceiard.api.service.document.DemandeDocument;
import net.checkconsulting.assuranceiard.api.service.document.DocumentsGeneresService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-31 étape 5 : initialisation, vérification IBAN et génération du contrat auto.
 * La génération crée en une transaction : mandat SEPA, contrat, échéancier, PDF.
 * L'e-mail est envoyé après la transaction (B8) depuis la resource.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaiementAutoService {

    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");
    private static final int DELAI_MAX_JOURS = 90;
    private static final String MSG_IBAN_INVALIDE = "IBAN invalide.";
    private static final String STATUT_EN_ATTENTE = "EN_ATTENTE_DE_SIGNATURE";

    private final SessionService sessionService;
    private final DevisAutoRepository devisAutos;
    private final VehiculeRepository vehicules;
    private final ConducteurRepository conducteurs;
    private final SouscripteurAutoRepository souscripteurs;
    private final ContratRepository contratsRepo;
    private final MandatSepaRepository mandats;
    private final EcheanceRepository echeances;
    private final DocumentsGeneresService documentsService;
    private final DocumentContenuRepository documentContenus;
    private final EnvoiEmailContrat emailService;
    private final IbanService ibanService;
    private final IbanBouchon ibanBouchon;
    private final PremierPrelevementService premierPrelevement;
    private final FormuleAutoService formuleAutoService;
    private final JdbcTemplate jdbc;
    private final Clock horloge;

    // -------------------------------------------------------------------------
    // A. Initialisation
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PaiementAutoInitResponse initialiser(Long devisAutoId, String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        DevisAuto devis = chargerDevis(devisAutoId);
        SouscripteurAuto souscripteur = souscripteurs.findByDevisAutoId(devisAutoId)
                .orElseThrow(() -> new VehiculeIntrouvableException("Souscripteur auto introuvable"));
        Vehicule vehicule = vehicules.findById(devis.getVehiculeId())
                .orElseThrow(() -> new VehiculeIntrouvableException("Véhicule introuvable"));

        List<OptionPrix> optionsPrix = construireOptionsPrix(devis, vehicule);
        BigDecimal franchise = franchiseParFormule(devis.getFormuleChoisie());

        boolean hamonApplicable = Boolean.TRUE.equals(devis.getAssurePlusUnAn());
        LocalDate dateFinAncienContrat = hamonApplicable
                ? LocalDate.ofInstant(horloge.instant(), FRANCE).plusMonths(1) : null;

        Conducteur conducteur = devis.getConducteurId() != null
                ? conducteurs.findById(devis.getConducteurId()).orElse(null) : null;
        Integer anneePermis = conducteur != null && conducteur.getDatePermis() != null
                ? conducteur.getDatePermis().getYear() : null;
        BigDecimal bonusMalus = conducteur != null ? conducteur.getCoefficientBonusMalus() : null;

        Optional<Contrat> contratExistant = contratsRepo.findByDevisAutoId(devisAutoId);

        log.info("layer=service Initialisation paiement auto : devisId={}", devisAutoId);
        return new PaiementAutoInitResponse(
                souscripteur.getPrenom(), souscripteur.getNom(),
                souscripteur.getAdresse(), souscripteur.getCodePostal(), souscripteur.getVille(),
                compte.getEmail(),
                vehicule.getMarque(), vehicule.getModele(), vehicule.getImmatriculation(),
                devis.getFormuleChoisie(), optionsPrix, franchise,
                devis.getPrixMensuelTtc(), devis.getPrixAnnuelTtc(),
                hamonApplicable, dateFinAncienContrat,
                MandatService.ICS, MandatService.CREANCIER,
                souscripteur.getPrenom() + " " + souscripteur.getNom(),
                contratExistant.map(Contrat::getNumero).orElse(null),
                contratExistant.map(c -> documentMandatSepaId(c.getId())).orElse(null),
                anneePermis, bonusMalus);
    }

    // -------------------------------------------------------------------------
    // B. Vérification IBAN (bouchon)
    // -------------------------------------------------------------------------

    public IbanVerificationResponse verifierIban(IbanVerificationRequest demande) {
        String iban = ibanService.normaliser(demande.iban());
        if (!ibanService.valide(iban)) {
            return IbanVerificationResponse.invalide(MSG_IBAN_INVALIDE);
        }
        return ibanBouchon.verifier(iban);
    }

    // -------------------------------------------------------------------------
    // C. Génération du contrat (transaction)
    // -------------------------------------------------------------------------

    @LogMetier(champs = {"demande.jourPrelevement"})
    @Transactional
    public ContratAutoGenerationResponse genererContrat(Long devisAutoId, PaiementAutoRequest demande,
            String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        DevisAuto devis = chargerDevis(devisAutoId);
        Vehicule vehicule = vehicules.findById(devis.getVehiculeId())
                .orElseThrow(() -> new VehiculeIntrouvableException("Véhicule introuvable"));

        SouscripteurAuto souscripteur = souscripteurs.findByDevisAutoId(devisAutoId)
                .orElseThrow(() -> new VehiculeIntrouvableException("Souscripteur auto introuvable"));

        // Idempotence : un seul contrat par devis auto (double-clic ou retour sur l'étape)
        Optional<Contrat> existant = contratsRepo.findByDevisAutoId(devisAutoId);
        if (existant.isPresent()) {
            Contrat c = existant.get();
            Long docId = documentMandatSepaId(c.getId());
            LocalDate premPrel = c.getMandatSepa() != null
                    ? c.getMandatSepa().getDatePremierPrelevement() : null;
            log.info("layer=service Contrat auto déjà existant : contrat={}", c.getNumero());
            return new ContratAutoGenerationResponse(c.getNumero(), c.getStatut(), docId,
                    compte.getEmail(), souscripteur.getPrenom(), souscripteur.getNom(),
                    premPrel, c.getCotisationMensuelle());
        }

        LocalDate aujourd_hui = LocalDate.ofInstant(horloge.instant(), FRANCE);
        verifierDateDebut(demande.dateDebut(), aujourd_hui, devis);
        verifierJour(demande.jourPrelevement());

        String ibanNormalise = ibanService.normaliser(demande.iban());
        if (!ibanService.valide(ibanNormalise)) {
            throw new InformationRefuseeException("iban", MSG_IBAN_INVALIDE);
        }

        LocalDate premierPrelevementDate = premierPrelevement.calculer(
                demande.dateDebut(), demande.jourPrelevement(), aujourd_hui);

        // Créer le mandat SEPA
        MandatSepa mandat = new MandatSepa();
        mandat.setCompteId(compte.getId());
        mandat.setRum(genererRum(aujourd_hui));
        mandat.setTitulaire(demande.titulaire().trim());
        mandat.setIbanFin(ibanService.fin(ibanNormalise));
        mandat.setIbanDebut(ibanService.debut(ibanNormalise));
        mandat.setBic(demande.bic());
        mandat.setBanque(demande.banque());
        mandat.setJourPrelevement(demande.jourPrelevement());
        mandat.setDateDebut(demande.dateDebut());
        mandat.setDatePremierPrelevement(premierPrelevementDate);
        mandat.setDateSignature(horloge.instant());
        mandat.setDateCreation(horloge.instant());
        mandat = mandats.saveAndFlush(mandat);

        // Créer le contrat (native INSERT pour contourner les colonnes insertable=false de l'entité JPA)
        BigDecimal franchise = franchiseParFormule(devis.getFormuleChoisie());
        boolean hamonApplicable = Boolean.TRUE.equals(devis.getAssurePlusUnAn());
        String numeroContrat = genererNumeroContrat(aujourd_hui);
        Long contratId = insererContratAuto(
                numeroContrat, compte.getId(), devis.getFormuleChoisie(),
                franchise != null ? franchise : BigDecimal.ZERO,
                devis.getPrixMensuelTtc().setScale(2, RoundingMode.HALF_UP),
                devis.getPrixAnnuelTtc().setScale(2, RoundingMode.HALF_UP),
                hamonApplicable, demande.dateDebut(),
                devis.getVehiculeId(), devis.getConducteurId(),
                mandat.getId(), demande.jourPrelevement(), devisAutoId);

        // Créer les 12 échéances
        creerEcheances(contratId, devis.getPrixMensuelTtc().setScale(2, RoundingMode.HALF_UP),
                premierPrelevementDate, demande.jourPrelevement(), aujourd_hui);

        // Générer le PDF mandat SEPA
        Document doc = documentsService.generer(DemandeDocument.duContrat(
                TypeDocument.MANDAT_SEPA, contratId, "Génération contrat auto"));

        log.info("layer=service Contrat auto généré : contrat={}, mandat={}, doc={}",
                numeroContrat, mandat.getId(), doc.getId());
        return new ContratAutoGenerationResponse(numeroContrat, STATUT_EN_ATTENTE, doc.getId(),
                compte.getEmail(), souscripteur.getPrenom(), souscripteur.getNom(),
                premierPrelevementDate, devis.getPrixMensuelTtc().setScale(2, RoundingMode.HALF_UP));
    }

    // -------------------------------------------------------------------------
    // D. Envoi de l'e-mail (hors transaction — appelé depuis la resource)
    // -------------------------------------------------------------------------

    public void envoyerEmailMandatSepa(Long documentId, String destinataire, String prenom, String nom,
            String numeroContrat, LocalDate premierPrelevementDate, BigDecimal montantMensuel) {
        try {
            byte[] pdf = documentContenus.findByDocumentId(documentId)
                    .map(DocumentContenu::getContenu).orElse(null);
            if (pdf == null) {
                log.warn("layer=service PDF mandat SEPA non disponible pour l'e-mail : doc={}", documentId);
                return;
            }
            // e-mail sans l'IBAN (RG6 : ne contient jamais l'IBAN)
            log.info("layer=service Envoi e-mail mandat SEPA : contrat={}", numeroContrat);
            emailService.envoyerMandatSepa(destinataire, prenom, nom, numeroContrat,
                    premierPrelevementDate, montantMensuel, pdf);
        } catch (Exception e) {
            log.error("layer=service Erreur envoi e-mail mandat SEPA : contrat={}", numeroContrat);
        }
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------

    private Long documentMandatSepaId(Long contratId) {
        return jdbc.queryForList(
                "SELECT id FROM document WHERE contrat_id = ? AND type = 'MANDAT_SEPA' AND statut = 'EN_VIGUEUR'",
                Long.class, contratId).stream().findFirst().orElse(null);
    }

    @SuppressWarnings("PMD.ExcessiveParameterList")
    private Long insererContratAuto(String numero, Long compteId, String formule, BigDecimal franchise,
            BigDecimal mensuelle, BigDecimal annuelle, boolean hamonApplicable, LocalDate dateEffet,
            Long vehiculeId, Long conducteurId, Long mandatId, Integer jourPrelevement, Long devisAutoId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update((con) -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO contrat (numero, statut, produit, compte_id, formule, franchise,
                        cotisation_mensuelle, cotisation_annuelle, resiliation_hamon, date_effet,
                        vehicule_id, conducteur_principal_id, mandat_sepa_id, frequence_paiement,
                        jour_prelevement, devis_auto_id, date_creation)
                    VALUES (?, 'EN_ATTENTE_DE_SIGNATURE', 'AUTO', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'MENSUEL', ?, ?, NOW())
                    """, new String[]{"id"});
            ps.setString(1, numero);
            ps.setLong(2, compteId);
            ps.setString(3, formule != null ? formule : "");
            ps.setBigDecimal(4, franchise);
            ps.setBigDecimal(5, mensuelle);
            ps.setBigDecimal(6, annuelle);
            ps.setBoolean(7, hamonApplicable);
            ps.setDate(8, Date.valueOf(dateEffet));
            ps.setLong(9, vehiculeId);
            ps.setLong(10, conducteurId);
            ps.setLong(11, mandatId);
            ps.setInt(12, jourPrelevement);
            ps.setLong(13, devisAutoId);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private void creerEcheances(Long contratId, BigDecimal montantMensuel, LocalDate premierPrelevementDate,
            int jourPrelevement, LocalDate aujourd_hui) {
        LocalDate dateEcheance = premierPrelevementDate;
        for (int i = 0; i < 12; i++) {
            Echeance e = new Echeance();
            e.setContratId(contratId);
            e.setDateEcheance(dateEcheance);
            // Prototype : pas de décomposition HT/taxe — montant TTC uniquement (simplification signalée)
            e.setMontantHt(montantMensuel);
            e.setTaxe(BigDecimal.ZERO);
            e.setMontantTtc(montantMensuel);
            echeances.save(e);
            dateEcheance = nextDateEcheance(dateEcheance, jourPrelevement);
        }
    }

    private static LocalDate nextDateEcheance(LocalDate courant, int jourPrelevement) {
        LocalDate suivant = courant.plusMonths(1).withDayOfMonth(jourPrelevement);
        return suivant.isBefore(courant) ? suivant.plusMonths(1) : suivant;
    }

    private String genererNumeroContrat(LocalDate aujourd_hui) {
        String prefixe = "AE-" + aujourd_hui.getYear();
        long seq = contratsRepo.prochainNumeroAnnee(prefixe);
        return prefixe + "-" + String.format("%06d", seq);
    }

    private String genererRum(LocalDate aujourd_hui) {
        String alea = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "AE-RUM-" + aujourd_hui.getYear() + "-" + alea;
    }

    private static BigDecimal franchiseParFormule(String formule) {
        if ("INTER".equals(formule)) return BigDecimal.valueOf(400);
        if ("TR".equals(formule)) return BigDecimal.valueOf(500);
        return null; // TIERS : sans franchise
    }

    private List<OptionPrix> construireOptionsPrix(DevisAuto devis, Vehicule vehicule) {
        if (devis.getOptionsChoisies() == null || devis.getOptionsChoisies().isBlank()) {
            return List.of();
        }
        List<String> codes = Arrays.asList(devis.getOptionsChoisies().split(","));
        try {
            Conducteur conducteur = devis.getConducteurId() != null
                    ? conducteurs.findById(devis.getConducteurId()).orElse(null) : null;
            if (conducteur != null && devis.getFormuleChoisie() != null) {
                Map<String, BigDecimal> prix = formuleAutoService.prixDesOptions(vehicule, conducteur,
                        devis.getFormuleChoisie());
                return codes.stream()
                        .map(code -> new OptionPrix(code, prix.get(code)))
                        .toList();
            }
        } catch (Exception e) {
            log.warn("layer=service Prix options indisponibles, devis={}", devis.getId());
        }
        return codes.stream().map(code -> new OptionPrix(code, null)).toList();
    }

    private void verifierDateDebut(LocalDate dateDebut, LocalDate aujourd_hui, DevisAuto devis) {
        if (!dateDebut.isAfter(aujourd_hui)) {
            throw new InformationRefuseeException("dateDebut", "Ce champ est obligatoire.");
        }
        if (dateDebut.isAfter(aujourd_hui.plusDays(DELAI_MAX_JOURS))) {
            throw new InformationRefuseeException("dateDebut",
                    "La date de début doit être comprise entre le "
                    + formaterDate(aujourd_hui.plusDays(1))
                    + " et le " + formaterDate(aujourd_hui.plusDays(DELAI_MAX_JOURS)) + ".");
        }
        if (Boolean.TRUE.equals(devis.getAssurePlusUnAn())) {
            LocalDate finAncienContrat = LocalDate.ofInstant(horloge.instant(), FRANCE).plusMonths(1);
            LocalDate minDateDebut = finAncienContrat.plusDays(1);
            if (dateDebut.isBefore(minDateDebut)) {
                throw new InformationRefuseeException("dateDebut",
                        "La date de début ne peut pas être antérieure au "
                        + formaterDate(minDateDebut) + ", fin de votre ancien contrat.");
            }
        }
    }

    private static String formaterDate(LocalDate date) {
        return String.format("%02d/%02d/%d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private static void verifierJour(int jour) {
        if (jour != 5 && jour != 10 && jour != 15) {
            throw new InformationRefuseeException("jourPrelevement", "Choisissez le 5, le 10 ou le 15 du mois.");
        }
    }

    private DevisAuto chargerDevis(Long id) {
        return devisAutos.findById(id)
                .orElseThrow(() -> new VehiculeIntrouvableException("Devis auto introuvable : id=" + id));
    }
}
