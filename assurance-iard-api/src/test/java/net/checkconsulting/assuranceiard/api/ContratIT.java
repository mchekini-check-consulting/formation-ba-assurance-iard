package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.model.MandatSepa;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DossierSouscriptionRepository;
import net.checkconsulting.assuranceiard.api.repository.MandatSepaRepository;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import net.checkconsulting.assuranceiard.api.service.EnvoiEmailContrat;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import net.checkconsulting.assuranceiard.api.client.MoteurTarifaireClient;
import net.checkconsulting.assuranceiard.api.dto.TarifResponse;
import net.checkconsulting.assuranceiard.api.model.TarifDevis;
import net.checkconsulting.assuranceiard.api.repository.TarifDevisRepository;

/** US-16 sur une vraie base : envoi du code, signature avec le vrai code, compteur d'essais. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ContratIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompteRepository comptes;

    @Autowired
    private RecommandationDevisRepository recommandations;

    @Autowired
    private DossierSouscriptionRepository dossiers;

    @Autowired
    private MandatSepaRepository mandats;

    @Autowired
    private ContratRepository contratRepository;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private TarifDevisRepository tarifsDevis;

    @MockitoBean
    private MoteurTarifaireClient moteur;

    @MockitoBean
    private EnvoiEmailContrat envoiEmail;

    private String jeton;
    private UUID reference;
    private DossierSouscription dossier;

    @BeforeEach
    void preparer() {
        Compte compte = new Compte();
        compte.setEmail(UUID.randomUUID() + "@exemple.fr");
        compte.setMotDePasseHash("hash");
        compte.setCivilite("madame");
        compte.setPrenom("Marie");
        compte.setNom("Durand");
        compte.setTelephone("0612345678");
        compte = comptes.save(compte);
        jeton = "Bearer " + sessionService.ouvrir(compte).jeton();

        RecommandationDevis recommandation = new RecommandationDevis();
        recommandation.setReference(UUID.randomUUID());
        recommandation.setRemboursement("neuf");
        recommandation.setPriorite("prix");
        recommandation.setFormule("confort");
        recommandation.setRaisons(List.of("raison"));
        recommandation.setOptions(List.of("assistance-depannage"));
        recommandation.setDateCreation(Instant.now());
        reference = recommandations.save(recommandation).getReference();

        dossier = new DossierSouscription();
        dossier.setCompte(compte);
        dossier.setReferenceDevis(reference);
        dossier.setColocataires(List.of());
        dossier.setDateCreation(Instant.now());
        dossier = dossiers.save(dossier);

        LocalDate dateDebut = LocalDate.now(ZoneId.of("Europe/Paris")).plusDays(1);
        MandatSepa mandat = new MandatSepa();
        mandat.setDossier(dossier);
        mandat.setRum("AE-RUM-TEST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase());
        mandat.setTitulaire("Marie Durand");
        mandat.setIbanFin("0189");
        mandat.setIbanDebut("FR76");
        mandat.setJourPrelevement(10);
        mandat.setDateDebut(dateDebut);
        mandat.setDatePremierPrelevement(dateDebut.withDayOfMonth(10).plusMonths(1));
        mandat.setDateSignature(Instant.now());
        mandat.setDateCreation(Instant.now());
        mandats.save(mandat);
    }

    private static final String CORPS_GENERATION = """
            {"referenceDevis":"%s","adresseLogement":"12 rue des Lilas",
            "codePostal":"75001","ville":"Paris","typeLogement":"appartement",
            "surface":45,"franchise":300,"resiliationHamon":false}
            """;

    @Test
    void genereLeContratEnvoieLeCodeEtLeSigne() throws Exception {
        retenirUnTarif(300);

        mockMvc.perform(post("/devis/contrat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, jeton)
                        .content(CORPS_GENERATION.formatted(reference)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_DE_SIGNATURE"))
                .andExpect(jsonPath("$.numero").value(org.hamcrest.Matchers.startsWith("AE-")));

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

        mockMvc.perform(post("/devis/contrat/{ref}/signature/code", reference)
                        .header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailMasque").exists());

        verify(envoiEmail).envoyer(anyString(), codeCaptor.capture());

        mockMvc.perform(post("/devis/contrat/{ref}/signature", reference)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, jeton)
                        .content("{\"code\":\"" + codeCaptor.getValue() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ACTIF"));

        assertThat(contratRepository.findByDossier(dossier))
                .isPresent()
                .hasValueSatisfying(c -> assertThat(c.getStatut()).isEqualTo("ACTIF"));
    }

    @Test
    void signatureRefuseeAvecUnMauvaisCode() throws Exception {
        retenirUnTarif(300);

        mockMvc.perform(post("/devis/contrat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, jeton)
                        .content(CORPS_GENERATION.formatted(reference)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/devis/contrat/{ref}/signature/code", reference)
                        .header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk());

        mockMvc.perform(post("/devis/contrat/{ref}/signature", reference)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, jeton)
                        .content("{\"code\":\"999999\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.essaisRestants").value(2));
    }

    @Test
    void signatureSansSessionEst401() throws Exception {
        mockMvc.perform(post("/devis/contrat/{ref}/signature", reference)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── Tests du compteur d'essais (Bug 2) ──────────────────────────────────

    @Test
    void deuxièmeMauvaisCodeIndique1EssaiRestant() throws Exception {
        genererContrat();
        envoyerCode();

        soumettreCode("000000").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.essaisRestants").value(2));

        soumettreCode("000000").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.essaisRestants").value(1));
    }

    @Test
    void troisièmeMauvaisCodeBloqueLaSignature() throws Exception {
        genererContrat();
        envoyerCode();

        soumettreCode("000000").andExpect(status().isUnprocessableEntity());
        soumettreCode("000000").andExpect(status().isUnprocessableEntity());
        soumettreCode("000000").andExpect(status().isConflict())
                .andExpect(jsonPath("$.motif").value("essais-epuises"));
    }

    @Test
    void quatrièmeTentativeEstImmédiatementBloquée() throws Exception {
        genererContrat();
        envoyerCode();

        soumettreCode("000000");
        soumettreCode("000000");
        soumettreCode("000000");
        soumettreCode("000000").andExpect(status().isConflict())
                .andExpect(jsonPath("$.motif").value("essais-epuises"));
    }

    @Test
    void renvoyerCodeRéinitialiseLeCompteur() throws Exception {
        genererContrat();
        envoyerCode();

        soumettreCode("000000");
        soumettreCode("000000");

        renvoyerCode();

        soumettreCode("000000").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.essaisRestants").value(2));
    }

    @Test
    void renvoyerCodeAprèsEpuisementPermetDeSignerAvecLeNouveauCode() throws Exception {
        genererContrat();
        envoyerCode();

        soumettreCode("000000");
        soumettreCode("000000");
        soumettreCode("000000");

        String nouveauCode = renvoyerCodeEtCapturerValeur();

        soumettreCode(nouveauCode).andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ACTIF"));
    }

    // ── Méthodes utilitaires ─────────────────────────────────────────────────

    private void genererContrat() throws Exception {
        retenirUnTarif(300);
        mockMvc.perform(post("/devis/contrat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, jeton)
                        .content(CORPS_GENERATION.formatted(reference)))
                .andExpect(status().isOk());
    }

    private void envoyerCode() throws Exception {
        mockMvc.perform(post("/devis/contrat/{ref}/signature/code", reference)
                        .header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk());
    }

    private void renvoyerCode() throws Exception {
        mockMvc.perform(post("/devis/contrat/{ref}/signature/code/renvoyer", reference)
                        .header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk());
    }

    private String renvoyerCodeEtCapturerValeur() throws Exception {
        clearInvocations(envoiEmail);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        mockMvc.perform(post("/devis/contrat/{ref}/signature/code/renvoyer", reference)
                        .header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk());
        verify(envoiEmail).envoyer(anyString(), captor.capture());
        return captor.getValue();
    }

    private org.springframework.test.web.servlet.ResultActions soumettreCode(String code) throws Exception {
        return mockMvc.perform(post("/devis/contrat/{ref}/signature", reference)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, jeton)
                .content("{\"code\":\"" + code + "\"}"));
    }

    private void retenirUnTarif(int franchise) {
        UUID idTarif = UUID.randomUUID();
        TarifDevis tarif = new TarifDevis();
        tarif.setReferenceDevis(reference);
        tarif.setIdTarif(idTarif);
        tarif.setCodePostal("75001");
        tarif.setTypeLogement("APPARTEMENT");
        tarif.setSurface(BigDecimal.valueOf(45));
        tarif.setFranchise(franchise);
        tarif.setDateCreation(Instant.now());
        tarifsDevis.save(tarif);
        when(moteur.reprendre(idTarif)).thenReturn(new TarifResponse(
                idTarif, "v", true, null, null, null,
                new TarifResponse.Detail("CONFORT", null, List.of(), null,
                        new BigDecimal("14.49"), new BigDecimal("173.88")),
                null, null, null, null, null, null));
    }

    @Test
    void leContratPrendLePrixDuTarifRetenu() throws Exception {
        retenirUnTarif(300);

        mockMvc.perform(post("/devis/contrat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, jeton)
                        .content(CORPS_GENERATION.formatted(reference)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cotisationMensuelle").value(14.49))
                .andExpect(jsonPath("$.cotisationAnnuelle").value(173.88));
    }

    @Test
    void unLogementDifferentDuTarifEstRefuseAvec409() throws Exception {
        retenirUnTarif(500);

        mockMvc.perform(post("/devis/contrat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, jeton)
                        .content(CORPS_GENERATION.formatted(reference)))
                .andExpect(status().isConflict());

        assertThat(contratRepository.findByDossier(dossier)).isEmpty();
    }
}
