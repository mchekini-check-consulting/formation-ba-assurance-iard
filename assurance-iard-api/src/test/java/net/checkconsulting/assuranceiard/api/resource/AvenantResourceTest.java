package net.checkconsulting.assuranceiard.api.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.checkconsulting.assuranceiard.api.dto.AcceptationAvenantRequest;
import net.checkconsulting.assuranceiard.api.dto.AvenantAccepteResponse;
import net.checkconsulting.assuranceiard.api.dto.AvenantResponse;
import net.checkconsulting.assuranceiard.api.dto.GarantiesContratResponse;
import net.checkconsulting.assuranceiard.api.exception.AvenantInconnuException;
import net.checkconsulting.assuranceiard.api.exception.AvenantInvalideException;
import net.checkconsulting.assuranceiard.api.exception.GestionErreurs;
import net.checkconsulting.assuranceiard.api.exception.ModificationGarantiesImpossibleException;
import net.checkconsulting.assuranceiard.api.service.avenant.AcceptationAvenantService;
import net.checkconsulting.assuranceiard.api.service.avenant.AvenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** US-23 : routes de l'avenant et leurs codes HTTP (le contrôle d'accès est testé sur base réelle, AvenantIT). */
@WebMvcTest(AvenantResource.class)
@Import(GestionErreurs.class)
class AvenantResourceTest {

    private static final String NUMERO = "AE-RECETTE-A03";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvenantService avenants;

    @MockitoBean
    private AcceptationAvenantService acceptations;

    private static AvenantResponse avenant(String statut) {
        return new AvenantResponse("AV-2026-000009", 1, statut, LocalDate.of(2026, 11, 27), new BigDecimal("47.50"),
                new BigDecimal("48.50"), List.of(), List.of(), null, null);
    }

    @Test
    void ac1GarantiesDuContrat() throws Exception {
        when(avenants.garanties(NUMERO)).thenReturn(new GarantiesContratResponse(NUMERO, "AUTO", "TR", "ACTIF",
                "Camille", new BigDecimal("47.50"), new BigDecimal("570.00"), "MENSUEL", List.of(
                        new GarantiesContratResponse.Garantie("RC", "Responsabilité civile", new BigDecimal("12.00"),
                                true, true, false, "obligatoire")), null));

        mockMvc.perform(get("/contrats/{numero}/garanties", NUMERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prenom").value("Camille"))
                .andExpect(jsonPath("$.garanties[0].modifiable").value(false));
    }

    @Test
    void ac6ContratQuiNEstPasActif422() throws Exception {
        when(avenants.garanties(NUMERO)).thenThrow(new ModificationGarantiesImpossibleException());

        mockMvc.perform(get("/contrats/{numero}/garanties", NUMERO))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("ne peuvent pas")));
    }

    @Test
    void ac3PreparerValideLeCorpsEtRefuseUneDemandeInvalide() throws Exception {
        mockMvc.perform(post("/contrats/{numero}/avenants", NUMERO).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        when(avenants.preparer(eq(NUMERO), any())).thenThrow(
                new AvenantInvalideException(AvenantInvalideException.AUCUN_CHANGEMENT));
        mockMvc.perform(post("/contrats/{numero}/avenants", NUMERO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"garanties\":[\"RC\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(AvenantInvalideException.AUCUN_CHANGEMENT));
    }

    @Test
    void ac4AccepterTransmetLAdresseDuClient() throws Exception {
        when(acceptations.accepter(eq(NUMERO), any(), any())).thenReturn(
                new AvenantAccepteResponse(avenant("ACCEPTE"), true, true, 2));

        mockMvc.perform(post("/contrats/{numero}/avenants/en-attente/acceptation", NUMERO)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"accepte\":true}")
                        .header("X-Forwarded-For", "203.0.113.7, 10.0.0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avenant.statut").value("ACCEPTE"))
                .andExpect(jsonPath("$.versionConditionsParticulieres").value(2));

        verify(acceptations).accepter(NUMERO, new AcceptationAvenantRequest(true), "203.0.113.7");
    }

    @Test
    void ac5AbandonnerEt404SansAvenantEnAttente() throws Exception {
        when(avenants.abandonner(NUMERO)).thenReturn(avenant("ABANDONNE"));
        mockMvc.perform(post("/contrats/{numero}/avenants/en-attente/abandon", NUMERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ABANDONNE"));

        when(avenants.abandonner(NUMERO)).thenThrow(new AvenantInconnuException());
        mockMvc.perform(post("/contrats/{numero}/avenants/en-attente/abandon", NUMERO))
                .andExpect(status().isNotFound());
    }

    @Test
    void adresseIpSansRelaisEtTropLongue() {
        MockHttpServletRequest requete = new MockHttpServletRequest();
        requete.setRemoteAddr("192.0.2.10");
        assertThat(AvenantResource.adresseIp(requete)).isEqualTo("192.0.2.10");

        requete.addHeader("X-Forwarded-For", "x".repeat(60));
        assertThat(AvenantResource.adresseIp(requete)).hasSize(45);
    }
}
