package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** US-14 sur une vraie base : migration V6, RUM stable, signature et absence de l'IBAN complet. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MandatIT {

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
    private SessionService sessionService;

    @Autowired
    private JdbcTemplate jdbc;

    private String jeton;
    private UUID reference;
    private DossierSouscription dossier;

    @BeforeEach
    void preparer() {
        Compte compte = new Compte();
        compte.setEmail(UUID.randomUUID() + "@exemple.fr");
        compte.setMotDePasseHash("hash");
        compte = comptes.save(compte);
        jeton = "Bearer " + sessionService.ouvrir(compte).jeton();

        RecommandationDevis recommandation = new RecommandationDevis();
        recommandation.setReference(UUID.randomUUID());
        recommandation.setRemboursement("neuf");
        recommandation.setPriorite("prix");
        recommandation.setFormule("confort");
        recommandation.setRaisons(List.of("raison"));
        recommandation.setOptions(List.of());
        recommandation.setDateCreation(Instant.now());
        reference = recommandations.save(recommandation).getReference();

        dossier = new DossierSouscription();
        dossier.setCompte(compte);
        dossier.setReferenceDevis(reference);
        dossier.setColocataires(List.of());
        dossier.setDateCreation(Instant.now());
        dossier = dossiers.save(dossier);
    }

    private ResultActions preparerMandat() throws Exception {
        return mockMvc.perform(post("/devis/mandats")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, jeton)
                .content("{\"referenceDevis\":\"" + reference + "\"}"));
    }

    private String rum() throws Exception {
        return JsonPath.read(preparerMandat().andReturn().getResponse().getContentAsString(), "$.rum");
    }

    @Test
    void uneRumEstAttribueeEtReste() throws Exception {
        String premiere = rum();

        assertThat(premiere).startsWith("AE-");
        assertThat(rum()).isEqualTo(premiere);
    }

    @Test
    void leMandatEstSigneSansConserverLIbanComplet() throws Exception {
        String rum = rum();
        LocalDate debut = LocalDate.now(ZoneId.of("Europe/Paris")).plusDays(1);

        mockMvc.perform(post("/devis/mandats/signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, jeton)
                        .content("""
                                {"referenceDevis":"%s","titulaire":"Marie Durand",
                                "iban":"FR76 3000 6000 0112 3456 7890 189","dateDebut":"%s",
                                "jourPrelevement":10,"autorisation":true}
                                """.formatted(reference, debut)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rum").value(rum));

        MandatSepa mandat = mandats.findByDossier(dossier).orElseThrow();
        assertThat(mandat.getIbanFin()).isEqualTo("0189");
        assertThat(mandat.getTitulaire()).isEqualTo("Marie Durand");
        assertThat(mandat.getJourPrelevement()).isEqualTo(10);
        assertThat(mandat.getDatePremierPrelevement()).isAfter(debut.minusDays(1));
        assertThat(mandat.getDateSignature()).isNotNull();
        assertThat(jdbc.queryForObject("select row_to_json(m)::text from mandat_sepa m where m.id = ?",
                        String.class, mandat.getId()))
                .doesNotContain("FR76", "30006");
    }

    @Test
    void sansSessionRienNEstPrepare() throws Exception {
        mockMvc.perform(post("/devis/mandats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceDevis\":\"" + reference + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void leDossierDUnAutreClientEstIntrouvable() throws Exception {
        Compte autre = new Compte();
        autre.setEmail(UUID.randomUUID() + "@exemple.fr");
        autre.setMotDePasseHash("hash");
        jeton = "Bearer " + sessionService.ouvrir(comptes.save(autre)).jeton();

        preparerMandat().andExpect(status().isNotFound());
    }
}
