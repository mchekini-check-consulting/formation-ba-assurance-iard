package net.checkconsulting.assuranceiard.api.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.DevisEnregistreResponse;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.exception.GestionErreurs;
import net.checkconsulting.assuranceiard.api.exception.SessionInvalideException;
import net.checkconsulting.assuranceiard.api.service.DevisEnregistreService;
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

@WebMvcTest(DevisEnregistreResource.class)
@Import(GestionErreurs.class)
class DevisEnregistreResourceTest {

    private static final String JETON = "Bearer jeton";
    private static final UUID REFERENCE = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final String CORPS = """
            {"reference":"7c9e6679-7425-40de-944b-e07fc1f90ae7",
            "logement":{"statut":"locataire","residence":"principale","typeLogement":"appartement",
            "adresse":"12 rue des Lilas","codePostal":"75001","ville":"Paris","surface":"60"},
            "formule":"confort","options":["objets-valeur"],"franchise":"300"}
            """;
    private static final DevisEnregistreResponse REPONSE = new DevisEnregistreResponse(REFERENCE, "confort",
            "appartement", 60, "12 rue des Lilas", "75001", "Paris", new BigDecimal("14.72"),
            new BigDecimal("176.64"), LocalDate.of(2026, 12, 5), false);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DevisEnregistreService service;

    private ResultActions envoyer(String adresse, String corps) throws Exception {
        return mockMvc.perform(post(adresse)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, JETON)
                .content(corps));
    }

    @Test
    void enregistreUnDevis() throws Exception {
        when(service.enregistrer(eq(JETON), any())).thenReturn(REPONSE);

        envoyer("/devis/enregistres", CORPS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateFinValidite").value("2026-12-05"));
    }

    @Test
    void refuseSansSession() throws Exception {
        when(service.enregistrer(any(), any())).thenThrow(new SessionInvalideException());

        mockMvc.perform(post("/devis/enregistres").contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "\"franchise\":\"300\"|\"franchise\":\"200\"",
        "\"formule\":\"confort\"|\"formule\":\"luxe\"",
        "\"objets-valeur\"|\"cadeau\"",
        "\"surface\":\"60\"|\"surface\":\"8\"",
        "\"surface\":\"60\"|\"surface\":\"1001\"",
        "\"codePostal\":\"75001\"|\"codePostal\":\"7500\"",
        "\"typeLogement\":\"appartement\"|\"typeLogement\":\"chateau\"",
        "\"adresse\":\"12 rue des Lilas\"|\"adresse\":\"\""
    })
    void refuseUneDemandeInvalide(String remplacement) throws Exception {
        String[] parties = remplacement.split("\\|");

        envoyer("/devis/enregistres", CORPS.replace(parties[0], parties[1])).andExpect(status().isBadRequest());
        envoyer("/devis/pdf", CORPS.replace(parties[0], parties[1])).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void listeLesDevis() throws Exception {
        when(service.lister(JETON)).thenReturn(List.of(REPONSE));

        mockMvc.perform(get("/devis/enregistres").header(HttpHeaders.AUTHORIZATION, JETON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].prixMensuel").value(14.72))
                .andExpect(jsonPath("$[0].expire").value(false));
    }

    @Test
    void repriseDUnDevisInconnuEst404() throws Exception {
        when(service.reprendre(JETON, REFERENCE)).thenThrow(new DevisInconnuException());

        envoyer("/devis/enregistres/" + REFERENCE + "/reprise", "").andExpect(status().isNotFound());
    }

    @Test
    void supprimeUnDevis() throws Exception {
        mockMvc.perform(delete("/devis/enregistres/" + REFERENCE).header(HttpHeaders.AUTHORIZATION, JETON))
                .andExpect(status().isNoContent());

        verify(service).supprimer(JETON, REFERENCE);
    }

    @Test
    void suppressionDUnDevisInconnuEst404() throws Exception {
        doThrow(new DevisInconnuException()).when(service).supprimer(JETON, REFERENCE);

        mockMvc.perform(delete("/devis/enregistres/" + REFERENCE).header(HttpHeaders.AUTHORIZATION, JETON))
                .andExpect(status().isNotFound());
    }

    @Test
    void rg2TelechargeLePdfSansSession() throws Exception {
        when(service.telecharger(any())).thenReturn(new byte[] {1, 2});

        mockMvc.perform(post("/devis/pdf").contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString(
                        "devis-assureasy.pdf")))
                .andExpect(content().bytes(new byte[] {1, 2}));
    }
}
