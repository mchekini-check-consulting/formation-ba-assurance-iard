package net.checkconsulting.assuranceiard.api.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.checkconsulting.assuranceiard.api.dto.ContratAutoGenerationResponse;
import net.checkconsulting.assuranceiard.api.dto.IbanVerificationResponse;
import net.checkconsulting.assuranceiard.api.dto.PaiementAutoInitResponse;
import net.checkconsulting.assuranceiard.api.exception.GestionErreurs;
import net.checkconsulting.assuranceiard.api.exception.SessionInvalideException;
import net.checkconsulting.assuranceiard.api.service.PaiementAutoService;
import net.checkconsulting.assuranceiard.api.service.SignatureAutoService;
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

@WebMvcTest(PaiementAutoResource.class)
@Import(GestionErreurs.class)
class PaiementAutoResourceTest {

    private static final String JETON = "Bearer jeton";
    private static final long DEVIS_ID = 42L;
    private static final String IBAN_TEST = "FR7630006000011234567890189";
    private static final String CORPS_CONTRAT = """
            {"dateDebut":"2026-12-15","titulaire":"Marie Durand",
            "iban":"FR7630006000011234567890189","bic":"BNPAFRPPXXX",
            "banque":"BNP Paribas","jourPrelevement":10,"autorisationPrelevement":true}
            """;
    private static final String CORPS_CONTRAT_SANS_BIC = """
            {"dateDebut":"2026-12-15","titulaire":"Marie Durand",
            "iban":"FR7630006000011234567890189","jourPrelevement":10,"autorisationPrelevement":true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaiementAutoService paiementAutoService;

    @MockitoBean
    private SignatureAutoService signatureAutoService;

    private ResultActions verifierIban(String corps) throws Exception {
        return mockMvc.perform(post("/v1/devis/auto/iban/verifier")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, JETON)
                .content(corps));
    }

    private ResultActions genererContrat(String corps) throws Exception {
        return mockMvc.perform(post("/v1/devis/auto/{id}/contrat", DEVIS_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, JETON)
                .content(corps));
    }

    @Test
    void initialiserRenvoieLesInformationsDeLEtape() throws Exception {
        PaiementAutoInitResponse init = new PaiementAutoInitResponse(
                "Marie", "Durand", "12 rue de la Paix", "75001", "Paris", "marie@test.fr",
                "Renault", "Clio", "AA-123-BB", "TR", List.of(),
                new BigDecimal("500.00"), new BigDecimal("55.00"), new BigDecimal("660.00"),
                false, null, "FR00ZZZ000000", "Assur'Easy", "Marie Durand", null, null,
                2010, new BigDecimal("0.68"));
        when(paiementAutoService.initialiser(DEVIS_ID, JETON)).thenReturn(init);

        mockMvc.perform(get("/v1/devis/auto/{id}/paiement", DEVIS_ID)
                        .header(HttpHeaders.AUTHORIZATION, JETON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.souscripteurNom").value("Durand"))
                .andExpect(jsonPath("$.formule").value("TR"))
                .andExpect(jsonPath("$.ics").value("FR00ZZZ000000"));
    }

    @Test
    void initialiserExigeUneSession() throws Exception {
        when(paiementAutoService.initialiser(eq(DEVIS_ID), any())).thenThrow(new SessionInvalideException());

        mockMvc.perform(get("/v1/devis/auto/{id}/paiement", DEVIS_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifierIbanValideRetourneLeResultat() throws Exception {
        when(paiementAutoService.verifierIban(any()))
                .thenReturn(IbanVerificationResponse.valide("•••• 0189", "BNPAFRPPXXX", "BNP Paribas"));

        verifierIban("{\"iban\":\"" + IBAN_TEST + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valide").value(true))
                .andExpect(jsonPath("$.zoneSepa").value(true))
                .andExpect(jsonPath("$.bic").value("BNPAFRPPXXX"));
    }

    @Test
    void verifierIbanHorsSepaRetourneZoneSepaFalse() throws Exception {
        when(paiementAutoService.verifierIban(any()))
                .thenReturn(IbanVerificationResponse.horsSepa(
                        "Cet IBAN n'est pas dans la zone SEPA : le prélèvement est impossible."));

        verifierIban("{\"iban\":\"BR1800360305000010009795493C1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valide").value(false))
                .andExpect(jsonPath("$.zoneSepa").value(false));
    }

    @Test
    void verifierIbanRefuseUnCorpsVide() throws Exception {
        verifierIban("{}").andExpect(status().isBadRequest());
        verifyNoInteractions(paiementAutoService);
    }

    @Test
    void genererContratRetourne201AvecLeNumero() throws Exception {
        ContratAutoGenerationResponse rep = new ContratAutoGenerationResponse(
                "AE-2026-000001", "EN_ATTENTE_DE_SIGNATURE", 7L,
                "marie@test.fr", "Marie", "Durand",
                LocalDate.of(2026, 12, 15), new BigDecimal("55.00"));
        when(paiementAutoService.genererContrat(eq(DEVIS_ID), any(), eq(JETON))).thenReturn(rep);

        genererContrat(CORPS_CONTRAT)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("AE-2026-000001"))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_DE_SIGNATURE"));
    }

    @Test
    void genererContratEnvoieLEmailApresCreeLeContrat() throws Exception {
        ContratAutoGenerationResponse rep = new ContratAutoGenerationResponse(
                "AE-2026-000001", "EN_ATTENTE_DE_SIGNATURE", 7L,
                "marie@test.fr", "Marie", "Durand",
                LocalDate.of(2026, 12, 15), new BigDecimal("55.00"));
        when(paiementAutoService.genererContrat(eq(DEVIS_ID), any(), eq(JETON))).thenReturn(rep);

        genererContrat(CORPS_CONTRAT).andExpect(status().isCreated());

        verify(paiementAutoService).envoyerEmailMandatSepa(
                eq(7L), eq("marie@test.fr"), eq("Marie"), eq("Durand"),
                eq("AE-2026-000001"), eq(LocalDate.of(2026, 12, 15)),
                any());
    }

    @Test
    void genererContratAccepteSansBicNiBanque() throws Exception {
        ContratAutoGenerationResponse rep = new ContratAutoGenerationResponse(
                "AE-2026-000002", "EN_ATTENTE_DE_SIGNATURE", 8L,
                "marie@test.fr", "Marie", "Durand",
                LocalDate.of(2026, 12, 15), new BigDecimal("55.00"));
        when(paiementAutoService.genererContrat(eq(DEVIS_ID), any(), eq(JETON))).thenReturn(rep);

        genererContrat(CORPS_CONTRAT_SANS_BIC).andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "\"dateDebut\":\"2026-12-15\"|\"dateDebut\":null",
        "\"titulaire\":\"Marie Durand\"|\"titulaire\":\" \"",
        "\"iban\":\"FR7630006000011234567890189\"|\"iban\":\"\"",
        "\"jourPrelevement\":10|\"jourPrelevement\":null",
        "\"autorisationPrelevement\":true|\"autorisationPrelevement\":false"
    })
    void genererContratRefuseUneDemandeInvalide(String remplacement) throws Exception {
        String[] parties = remplacement.split("\\|");

        genererContrat(CORPS_CONTRAT.replace(parties[0], parties[1]))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paiementAutoService);
    }

    @Test
    void genererContratExigeUneSession() throws Exception {
        when(paiementAutoService.genererContrat(any(), any(), any())).thenThrow(new SessionInvalideException());

        genererContrat(CORPS_CONTRAT).andExpect(status().isUnauthorized());
    }

    @Test
    void lIbanNeFigureJamasDansLaReponseDeVerification() throws Exception {
        when(paiementAutoService.verifierIban(any()))
                .thenReturn(IbanVerificationResponse.valide("•••• 0189", "BNPAFRPPXXX", "BNP Paribas"));

        String reponse = verifierIban("{\"iban\":\"" + IBAN_TEST + "\"}")
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(reponse).doesNotContain("FR763000600001");
    }
}
