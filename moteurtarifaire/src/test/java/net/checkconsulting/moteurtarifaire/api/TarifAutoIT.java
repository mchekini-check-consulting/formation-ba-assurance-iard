package net.checkconsulting.moteurtarifaire.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TarifAutoIT {

    private static final String ADRESSE = "/v1/tarifs/auto";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private Clock horloge;

    private ResultActions envoyer(Map<String, Object> corps) throws Exception {
        return mockMvc.perform(post(ADRESSE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(corps)));
    }

    private JsonNode corps(ResultActions reponse) throws Exception {
        return jsonMapper.readTree(reponse.andReturn().getResponse().getContentAsString());
    }

    private Map<String, Object> demandeBase() {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("anneePermis", 2015);
        corps.put("bonusMalus", new BigDecimal("1.00"));
        corps.put("kmAnnuels", 10000);
        corps.put("usage", "PRIVE");
        corps.put("stationnement", "GARAGE_FERME");
        corps.put("valeurVehicule", new BigDecimal("8000"));
        return corps;
    }

    @Test
    void ca1CalculNominalRenvoieLesTroisFormules() throws Exception {
        envoyer(demandeBase())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifable").value(true))
                .andExpect(jsonPath("$.versionGrille").value("AUTO-2026.1"))
                .andExpect(jsonPath("$.prix", hasSize(3)))
                .andExpect(jsonPath("$.prix[0].formule").value("TIERS"))
                .andExpect(jsonPath("$.prix[1].formule").value("INTER"))
                .andExpect(jsonPath("$.prix[2].formule").value("TR"))
                .andExpect(jsonPath("$.formuleConseillee").value("TIERS"))
                .andExpect(jsonPath("$.dateValidite")
                        .value(LocalDate.now(horloge).plusDays(30).toString()));
    }

    @Test
    void ca2UnVehiculeHautDeGammeConseilleTr() throws Exception {
        Map<String, Object> corps = demandeBase();
        corps.put("valeurVehicule", new BigDecimal("30000"));

        envoyer(corps)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formuleConseillee").value("TR"));
    }

    @Test
    void ca2UnGrandKilometrageConseillInter() throws Exception {
        Map<String, Object> corps = demandeBase();
        corps.put("kmAnnuels", 20000);

        envoyer(corps)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formuleConseillee").value("INTER"));
    }

    @Test
    void ca3DetailAvecFormuleTiers() throws Exception {
        Map<String, Object> corps = demandeBase();
        corps.put("formule", "TIERS");

        envoyer(corps)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.detail.formule").value("TIERS"));
    }

    @Test
    void ca3SansFormulePasDeDetail() throws Exception {
        envoyer(demandeBase())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail").doesNotExist());
    }

    @Test
    void ca4UnNoviceAUneSuprime() throws Exception {
        Map<String, Object> corpsNovice = demandeBase();
        corpsNovice.put("anneePermis", LocalDate.now(horloge).getYear() - 1);

        double prixNovice = corps(envoyer(corpsNovice)).get("prix").get(0).get("mensuel").asDouble();
        double prixNormal = corps(envoyer(demandeBase())).get("prix").get(0).get("mensuel").asDouble();

        assertThat(prixNovice).isGreaterThan(prixNormal);
    }

    @Test
    void ca5UnChampObligatoireAbsentDonne400() throws Exception {
        Map<String, Object> corps = demandeBase();
        corps.remove("anneePermis");

        envoyer(corps)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs[0].champ").value("anneePermis"));
    }

    @Test
    void nonRegressionHabitationNEstPasAffectee() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/v1/tarifs/habitation'].post").exists())
                .andExpect(jsonPath("$.paths['" + ADRESSE + "'].post").exists());
    }
}
