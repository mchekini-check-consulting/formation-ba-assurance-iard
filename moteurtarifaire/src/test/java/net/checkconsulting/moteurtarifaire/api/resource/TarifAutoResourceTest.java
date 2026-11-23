package net.checkconsulting.moteurtarifaire.api.resource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.checkconsulting.moteurtarifaire.api.dto.TarifAutoResponse;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleIndisponibleException;
import net.checkconsulting.moteurtarifaire.api.service.TarificateurAuto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(TarifAutoResource.class)
class TarifAutoResourceTest {

    private static final String ADRESSE = "/v1/tarifs/auto";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private TarificateurAuto tarificateurAuto;

    private Map<String, Object> corpsValide() {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("anneePermis", 2015);
        corps.put("bonusMalus", new BigDecimal("1.00"));
        corps.put("kmAnnuels", 10000);
        corps.put("usage", "PRIVE");
        corps.put("stationnement", "GARAGE_FERME");
        corps.put("valeurVehicule", new BigDecimal("8000"));
        return corps;
    }

    private ResultActions envoyer(Map<String, Object> corps) throws Exception {
        return mockMvc.perform(post(ADRESSE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(corps)));
    }

    @Test
    void unAppelValideRenvoieLeResultatDuService() throws Exception {
        UUID idTarif = UUID.randomUUID();
        when(tarificateurAuto.calculer(any()))
                .thenReturn(TarifAutoResponse.builder().idTarif(idTarif).tarifable(true).build());

        envoyer(corpsValide())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTarif").value(idTarif.toString()))
                .andExpect(jsonPath("$.tarifable").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"anneePermis", "bonusMalus", "kmAnnuels", "usage", "stationnement", "valeurVehicule"})
    void unChampObligatoireAbsentEstSignale(String champ) throws Exception {
        Map<String, Object> corps = corpsValide();
        corps.remove(champ);

        envoyer(corps)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs[?(@.champ == '" + champ + "')].message").isNotEmpty());
        verify(tarificateurAuto, never()).calculer(any());
    }

    @Test
    void tousLesChampsAbsentsDonnePlusieursErreurs() throws Exception {
        envoyer(Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs", hasSize(6)));
    }

    @Test
    void unBonusMalusTropBasEstRefuse() throws Exception {
        Map<String, Object> corps = corpsValide();
        corps.put("bonusMalus", new BigDecimal("0.49"));

        envoyer(corps)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs[?(@.champ == 'bonusMalus')]").isNotEmpty());
    }

    @Test
    void unBonusMalusTropHautEstRefuse() throws Exception {
        Map<String, Object> corps = corpsValide();
        corps.put("bonusMalus", new BigDecimal("3.51"));

        envoyer(corps)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs[?(@.champ == 'bonusMalus')]").isNotEmpty());
    }

    @Test
    void uneValeurDUsageInconnueListeLesValeursPossibles() throws Exception {
        Map<String, Object> corps = corpsValide();
        corps.put("usage", "INCONNU");

        envoyer(corps)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs[0].message").value(containsString("PRIVE")));
    }

    @Test
    void sansGrilleLeServiceEstIndisponible() throws Exception {
        when(tarificateurAuto.calculer(any())).thenThrow(new GrilleIndisponibleException(LocalDate.EPOCH));

        envoyer(corpsValide())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(containsString("grille")));
    }

    @Test
    void unJsonIllisibleEstRefuse() throws Exception {
        mockMvc.perform(post(ADRESSE).contentType(MediaType.APPLICATION_JSON).content("{pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("JSON")));
    }
}
