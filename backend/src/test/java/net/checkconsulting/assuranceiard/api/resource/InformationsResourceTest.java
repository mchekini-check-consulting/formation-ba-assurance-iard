package net.checkconsulting.assuranceiard.api.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import net.checkconsulting.assuranceiard.api.dto.IdentiteResponse;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.exception.GestionErreurs;
import net.checkconsulting.assuranceiard.api.exception.InformationRefuseeException;
import net.checkconsulting.assuranceiard.api.exception.SessionInvalideException;
import net.checkconsulting.assuranceiard.api.service.InformationsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@WebMvcTest(InformationsResource.class)
@Import(GestionErreurs.class)
class InformationsResourceTest {

    private static final String JETON = "Bearer jeton";
    private static final String VALIDE = """
            {"referenceDevis":"7c9e6679-7425-40de-944b-e07fc1f90ae7","civilite":"madame","nom":"Durand",
            "prenom":"Marie","dateNaissance":"1990-01-31","telephone":"0612345678",
            "typePieceIdentite":"passeport","typeJustificatifDomicile":"gaz","colocataires":["Paul Martin"]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InformationsService informationsService;

    private ResultActions poster(String informations) throws Exception {
        return mockMvc.perform(multipart("/devis/informations")
                .file(new MockMultipartFile(
                        "informations", "", MediaType.APPLICATION_JSON_VALUE,
                        informations.getBytes(StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("pieceIdentite", "carte.pdf", null, new byte[] {1}))
                .file(new MockMultipartFile("justificatifDomicile", "facture.pdf", null, new byte[] {2}))
                .header(HttpHeaders.AUTHORIZATION, JETON));
    }

    @Test
    void lireRenvoieLIdentiteDuCompte() throws Exception {
        when(informationsService.lire(JETON)).thenReturn(
                new IdentiteResponse("madame", "Durand", "Marie", LocalDate.of(1990, 1, 31), "0612345678"));

        mockMvc.perform(get("/devis/informations").header(HttpHeaders.AUTHORIZATION, JETON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Durand"))
                .andExpect(jsonPath("$.dateNaissance").value("1990-01-31"));
    }

    @Test
    void lireExigeUneSession() throws Exception {
        when(informationsService.lire(null)).thenThrow(new SessionInvalideException());

        mockMvc.perform(get("/devis/informations")).andExpect(status().isUnauthorized());
    }

    @Test
    void enregistreLesInformationsEtLesFichiers() throws Exception {
        poster(VALIDE).andExpect(status().isNoContent());

        verify(informationsService).enregistrer(eq(JETON), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "\"civilite\":\"madame\"|\"civilite\":\"autre\"",
        "\"nom\":\"Durand\"|\"nom\":\"  \"",
        "\"prenom\":\"Marie\"|\"prenom\":\"\"",
        "\"dateNaissance\":\"1990-01-31\"|\"dateNaissance\":null",
        "\"telephone\":\"0612345678\"|\"telephone\":\"061234567\"",
        "\"telephone\":\"0612345678\"|\"telephone\":\"06123456789\"",
        "\"telephone\":\"0612345678\"|\"telephone\":\"0012345678\"",
        "\"telephone\":\"0612345678\"|\"telephone\":\"+33612345678\"",
        "\"typePieceIdentite\":\"passeport\"|\"typePieceIdentite\":\"permis\"",
        "\"typeJustificatifDomicile\":\"gaz\"|\"typeJustificatifDomicile\":\"impots\"",
        "[\"Paul Martin\"]|[\" \"]",
        "\"referenceDevis\":\"7c9e6679-7425-40de-944b-e07fc1f90ae7\"|\"referenceDevis\":null"
    })
    void refuseUneInformationInvalide(String remplacement) throws Exception {
        String[] parties = remplacement.split("\\|");

        poster(VALIDE.replace(parties[0], parties[1])).andExpect(status().isBadRequest());

        verifyNoInteractions(informationsService);
    }

    @Test
    void refuseUnEnvoiSansFichier() throws Exception {
        mockMvc.perform(multipart("/devis/informations").file(new MockMultipartFile(
                        "informations", "", MediaType.APPLICATION_JSON_VALUE,
                        VALIDE.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(informationsService);
    }

    @Test
    void indiqueLeChampRefuse() throws Exception {
        doThrow(new InformationRefuseeException("pieceIdentite", "Ce format n'est pas accepté."))
                .when(informationsService).enregistrer(any(), any(), any(), any());

        poster(VALIDE)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champ").value("pieceIdentite"))
                .andExpect(jsonPath("$.detail").value("Ce format n'est pas accepté."));
    }

    @Test
    void repondSessionInvalideSansSession() throws Exception {
        doThrow(new SessionInvalideException())
                .when(informationsService).enregistrer(any(), any(), any(), any());

        poster(VALIDE).andExpect(status().isUnauthorized());
    }

    @Test
    void repondDevisInconnu() throws Exception {
        doThrow(new DevisInconnuException())
                .when(informationsService).enregistrer(any(), any(), any(), any());

        poster(VALIDE).andExpect(status().isNotFound());
    }

    @Test
    void repondTropVolumineuxAuDelaDeLaLimiteDEnvoi() throws Exception {
        doThrow(new MaxUploadSizeExceededException(1))
                .when(informationsService).enregistrer(any(), any(), any(), any());

        poster(VALIDE).andExpect(status().isContentTooLarge());
    }
}
