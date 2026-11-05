package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
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
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** US-15 sur une vraie base : génération, signature avec le code dev, rejet d'un mauvais code. */
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
            "surface":45,"franchise":300,"cotisationMensuelle":18.50,"resiliationHamon":false}
            """;

    @Test
    void genereLeContratEtLeSigneAvecLeCodeDev() throws Exception {
        mockMvc.perform(post("/devis/contrat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, jeton)
                        .content(CORPS_GENERATION.formatted(reference)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_DE_SIGNATURE"))
                .andExpect(jsonPath("$.numero").value(org.hamcrest.Matchers.startsWith("AE-")));

        mockMvc.perform(post("/devis/contrat/{ref}/signature", reference)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, jeton)
                        .content("{\"code\":\"" + "123456" + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("SIGNE"));

        assertThat(contratRepository.findByDossier(dossier))
                .isPresent()
                .hasValueSatisfying(c -> assertThat(c.getStatut()).isEqualTo("SIGNE"));
    }

    @Test
    void signatureRefuseeAvecUnMauvaisCode() throws Exception {
        mockMvc.perform(post("/devis/contrat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, jeton)
                        .content(CORPS_GENERATION.formatted(reference)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/devis/contrat/{ref}/signature", reference)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, jeton)
                        .content("{\"code\":\"999999\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void signatureSansSessionEst401() throws Exception {
        mockMvc.perform(post("/devis/contrat/{ref}/signature", reference)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + "123456" + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
