package net.checkconsulting.assuranceiard.api.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import net.checkconsulting.assuranceiard.api.dto.InformationsAutoResponse;
import net.checkconsulting.assuranceiard.api.exception.GestionErreurs;
import net.checkconsulting.assuranceiard.api.exception.InformationRefuseeException;
import net.checkconsulting.assuranceiard.api.exception.SessionInvalideException;
import net.checkconsulting.assuranceiard.api.service.InformationsAutoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(InformationsAutoResource.class)
@Import(GestionErreurs.class)
class InformationsAutoResourceTest {

    private static final String JETON = "Bearer jeton";
    private static final long DEVIS_ID = 42L;
    private static final String VALIDE = """
            {"civilite":"madame","nom":"Durand","prenom":"Marie",
            "dateNaissance":"1990-01-31","telephone":"0612345678",
            "adresse":"12 rue de la Paix","codePostal":"75001","ville":"Paris"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InformationsAutoService informationsAutoService;

    private ResultActions poster(String corps) throws Exception {
        return mockMvc.perform(post("/v1/devis/auto/{id}/souscripteur", DEVIS_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps)
                .header(HttpHeaders.AUTHORIZATION, JETON));
    }

    @Test
    void lireRenvoieLePreremplissage() throws Exception {
        when(informationsAutoService.lire(DEVIS_ID, JETON)).thenReturn(
                new InformationsAutoResponse("madame", "Durand", "Marie",
                        LocalDate.of(1990, 1, 31), "0612345678", null, null, null));

        mockMvc.perform(get("/v1/devis/auto/{id}/souscripteur", DEVIS_ID)
                        .header(HttpHeaders.AUTHORIZATION, JETON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Durand"))
                .andExpect(jsonPath("$.civilite").value("madame"));
    }

    @Test
    void lireExigeUneSession() throws Exception {
        when(informationsAutoService.lire(eq(DEVIS_ID), any())).thenThrow(new SessionInvalideException());

        mockMvc.perform(get("/v1/devis/auto/{id}/souscripteur", DEVIS_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void enregistreLesInformations() throws Exception {
        poster(VALIDE).andExpect(status().isNoContent());

        verify(informationsAutoService).enregistrer(eq(DEVIS_ID), any(), eq(JETON));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "\"civilite\":\"madame\"|\"civilite\":\"autre\"",
        "\"nom\":\"Durand\"|\"nom\":\"  \"",
        "\"prenom\":\"Marie\"|\"prenom\":\"\"",
        "\"dateNaissance\":\"1990-01-31\"|\"dateNaissance\":null",
        "\"telephone\":\"0612345678\"|\"telephone\":\"0512345678\"",
        "\"telephone\":\"0612345678\"|\"telephone\":\"061234567\"",
        "\"telephone\":\"0612345678\"|\"telephone\":\"06123456789\"",
        "\"adresse\":\"12 rue de la Paix\"|\"adresse\":\"ab\"",
        "\"codePostal\":\"75001\"|\"codePostal\":\"7500\"",
        "\"codePostal\":\"75001\"|\"codePostal\":\"750012\"",
        "\"codePostal\":\"75001\"|\"codePostal\":\"7500A\"",
        "\"ville\":\"Paris\"|\"ville\":\"P\""
    })
    void refuseUneInformationInvalide(String remplacement) throws Exception {
        String[] parties = remplacement.split("\\|");

        poster(VALIDE.replace(parties[0], parties[1])).andExpect(status().isBadRequest());

        verifyNoInteractions(informationsAutoService);
    }

    @Test
    void indiqueLeChampRefuse() throws Exception {
        doThrow(new InformationRefuseeException("dateNaissance", "Vous devez avoir au moins 18 ans."))
                .when(informationsAutoService).enregistrer(any(), any(), any());

        poster(VALIDE)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champ").value("dateNaissance"))
                .andExpect(jsonPath("$.detail").value("Vous devez avoir au moins 18 ans."));
    }

    @Test
    void repondSessionInvalideSansSession() throws Exception {
        doThrow(new SessionInvalideException())
                .when(informationsAutoService).enregistrer(any(), any(), any());

        poster(VALIDE).andExpect(status().isUnauthorized());
    }
}
