package net.checkconsulting.assuranceiard.api.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.MandatPreparationResponse;
import net.checkconsulting.assuranceiard.api.dto.MandatSigneResponse;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.exception.GestionErreurs;
import net.checkconsulting.assuranceiard.api.exception.InformationRefuseeException;
import net.checkconsulting.assuranceiard.api.exception.SessionInvalideException;
import net.checkconsulting.assuranceiard.api.service.MandatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(MandatResource.class)
@Import(GestionErreurs.class)
@ExtendWith(OutputCaptureExtension.class)
class MandatResourceTest {

    private static final String JETON = "Bearer jeton";
    private static final UUID REFERENCE = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final String IBAN_TEST = "FR7630006000011234567890189";
    private static final String VALIDE = """
            {"referenceDevis":"7c9e6679-7425-40de-944b-e07fc1f90ae7","titulaire":"Marie Durand",
            "iban":"FR7630006000011234567890189","dateDebut":"2026-12-01","jourPrelevement":10,"autorisation":true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MandatService mandatService;

    private ResultActions signer(String corps) throws Exception {
        return mockMvc.perform(post("/devis/mandats/signature")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, JETON)
                .content(corps));
    }

    private ResultActions preparer(String corps) throws Exception {
        return mockMvc.perform(post("/devis/mandats")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, JETON)
                .content(corps));
    }

    @Test
    void prepareLeMandat() throws Exception {
        when(mandatService.preparer(JETON, REFERENCE))
                .thenReturn(new MandatPreparationResponse("Assur'Easy", "FR00ZZZ000000", "AE-2026-ABCDEFGH"));

        preparer("{\"referenceDevis\":\"" + REFERENCE + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creancier").value("Assur'Easy"))
                .andExpect(jsonPath("$.ics").value("FR00ZZZ000000"))
                .andExpect(jsonPath("$.rum").value("AE-2026-ABCDEFGH"));
    }

    @Test
    void preparerExigeUneReference() throws Exception {
        preparer("{}").andExpect(status().isBadRequest());

        verifyNoInteractions(mandatService);
    }

    @Test
    void preparerRefuseUnDossierInconnu() throws Exception {
        when(mandatService.preparer(JETON, REFERENCE)).thenThrow(new DevisInconnuException());

        preparer("{\"referenceDevis\":\"" + REFERENCE + "\"}").andExpect(status().isNotFound());
    }

    @Test
    void signeLeMandat() throws Exception {
        when(mandatService.signer(eq(JETON), any()))
                .thenReturn(new MandatSigneResponse("AE-2026-ABCDEFGH", LocalDate.of(2026, 12, 10)));

        signer(VALIDE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rum").value("AE-2026-ABCDEFGH"))
                .andExpect(jsonPath("$.datePremierPrelevement").value("2026-12-10"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "\"autorisation\":true|\"autorisation\":false",
        "\"autorisation\":true|\"autorisation\":null",
        "\"titulaire\":\"Marie Durand\"|\"titulaire\":\" \"",
        "\"iban\":\"FR7630006000011234567890189\"|\"iban\":\"\"",
        "\"dateDebut\":\"2026-12-01\"|\"dateDebut\":null",
        "\"jourPrelevement\":10|\"jourPrelevement\":null",
        "\"referenceDevis\":\"7c9e6679-7425-40de-944b-e07fc1f90ae7\"|\"referenceDevis\":null"
    })
    void refuseUneDemandeInvalide(String remplacement) throws Exception {
        String[] parties = remplacement.split("\\|");

        signer(VALIDE.replace(parties[0], parties[1])).andExpect(status().isBadRequest());

        verifyNoInteractions(mandatService);
    }

    @Test
    void refuseUneSignatureSansSession() throws Exception {
        when(mandatService.signer(any(), any())).thenThrow(new SessionInvalideException());

        signer(VALIDE).andExpect(status().isUnauthorized());
    }

    @Test
    void indiqueLeChampRefuse() throws Exception {
        when(mandatService.signer(any(), any()))
                .thenThrow(new InformationRefuseeException("iban", "Cet IBAN n'est pas valide."));

        signer(VALIDE)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champ").value("iban"));
    }

    @Test
    void lIbanNeFigureJamaisDansLesLogsNiDansLaReponse(CapturedOutput journaux) throws Exception {
        when(mandatService.signer(any(), any()))
                .thenThrow(new InformationRefuseeException("iban", "Cet IBAN n'est pas valide."));

        String reponse = signer(VALIDE).andReturn().getResponse().getContentAsString();
        String refuse = signer(VALIDE.replace("\"autorisation\":true", "\"autorisation\":false"))
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(journaux.getAll()).doesNotContain(IBAN_TEST);
        org.assertj.core.api.Assertions.assertThat(reponse + refuse).doesNotContain(IBAN_TEST);
    }
}
