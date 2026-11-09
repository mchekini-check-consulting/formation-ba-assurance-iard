package net.checkconsulting.assuranceiard.api.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.ContratResponse;
import net.checkconsulting.assuranceiard.api.exception.CodeSignatureIncorrectException;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.exception.GestionErreurs;
import net.checkconsulting.assuranceiard.api.exception.MandatNonSigneException;
import net.checkconsulting.assuranceiard.api.exception.SessionInvalideException;
import net.checkconsulting.assuranceiard.api.service.ContratService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(ContratResource.class)
@Import(GestionErreurs.class)
class ContratResourceTest {

    private static final String JETON = "Bearer jeton";
    private static final UUID REFERENCE = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final String CORPS_VALIDE = """
            {"referenceDevis":"7c9e6679-7425-40de-944b-e07fc1f90ae7",
            "adresseLogement":"12 rue des Lilas","codePostal":"75001","ville":"Paris",
            "typeLogement":"appartement","surface":45,"franchise":300,
            "resiliationHamon":false}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContratService contratService;

    private ResultActions generer(String corps) throws Exception {
        return mockMvc.perform(post("/devis/contrat")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, JETON)
                .content(corps));
    }

    private ResultActions obtenir(UUID reference) throws Exception {
        return mockMvc.perform(get("/devis/contrat/" + reference)
                .header(HttpHeaders.AUTHORIZATION, JETON));
    }

    private ResultActions signer(UUID reference, String corps) throws Exception {
        return mockMvc.perform(post("/devis/contrat/" + reference + "/signature")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, JETON)
                .content(corps));
    }

    @Test
    void genereLeContrat() throws Exception {
        when(contratService.generer(eq(JETON), any())).thenReturn(uneReponse());

        generer(CORPS_VALIDE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("AE-2026-000001"))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_DE_SIGNATURE"))
                .andExpect(jsonPath("$.formule").value("confort"));
    }

    @Test
    void retourne404SiDevisInconnu() throws Exception {
        when(contratService.generer(eq(JETON), any())).thenThrow(new DevisInconnuException());

        generer(CORPS_VALIDE).andExpect(status().isNotFound());
    }

    @Test
    void retourne401SiSessionInvalide() throws Exception {
        when(contratService.generer(eq(JETON), any())).thenThrow(new SessionInvalideException());

        generer(CORPS_VALIDE).andExpect(status().isUnauthorized());
    }

    @Test
    void retourne422SiMandatNonSigne() throws Exception {
        when(contratService.generer(eq(JETON), any())).thenThrow(new MandatNonSigneException());

        generer(CORPS_VALIDE).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejetteSiCorpsInvalide() throws Exception {
        generer("{}")
                .andExpect(status().isBadRequest());
        verifyNoInteractions(contratService);
    }

    @Test
    void obtientLeContrat() throws Exception {
        when(contratService.obtenir(JETON, REFERENCE)).thenReturn(uneReponse());

        obtenir(REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("AE-2026-000001"));
    }

    @Test
    void retourne404SiContratIntrouvable() throws Exception {
        when(contratService.obtenir(JETON, REFERENCE)).thenThrow(new DevisInconnuException());

        obtenir(REFERENCE).andExpect(status().isNotFound());
    }

    @Test
    void signeLeContrat() throws Exception {
        ContratResponse signee = new ContratResponse(
                "AE-2026-000001", "SIGNE", "M.", "Jean", "Dupont", "0612345678",
                "12 rue des Lilas", "75001", "Paris", "appartement", 45,
                "confort", List.of("assistance-depannage"), 300,
                new BigDecimal("18.50"), new BigDecimal("222.00"), 10,
                "7890", "FR76", "AE-2026-ABCDEF12",
                LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 1),
                false, LocalDate.of(2026, 11, 5));
        when(contratService.signer(eq(JETON), eq(REFERENCE), any())).thenReturn(signee);

        signer(REFERENCE, "{\"code\":\"123456\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("SIGNE"));
    }

    @Test
    void retourne422SiCodeSignatureIncorrect() throws Exception {
        when(contratService.signer(eq(JETON), eq(REFERENCE), any()))
                .thenThrow(new CodeSignatureIncorrectException());

        signer(REFERENCE, "{\"code\":\"999999\"}")
                .andExpect(status().isUnprocessableEntity());
    }

    private ContratResponse uneReponse() {
        return new ContratResponse(
                "AE-2026-000001",
                "EN_ATTENTE_DE_SIGNATURE",
                "M.",
                "Jean",
                "Dupont",
                "0612345678",
                "12 rue des Lilas",
                "75001",
                "Paris",
                "appartement",
                45,
                "confort",
                List.of("assistance-depannage"),
                300,
                new BigDecimal("18.50"),
                new BigDecimal("222.00"),
                10,
                "7890",
                "FR76",
                "AE-2026-ABCDEF12",
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 1),
                false,
                LocalDate.of(2026, 11, 5));
    }
}
