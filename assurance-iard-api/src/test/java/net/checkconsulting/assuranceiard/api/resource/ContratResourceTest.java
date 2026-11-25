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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.DemandeFactureRequest;
import net.checkconsulting.assuranceiard.api.dto.FactureGenereeResponse;
import net.checkconsulting.assuranceiard.api.dto.MesFacturesResponse;
import net.checkconsulting.assuranceiard.api.exception.FactureImpossibleException;
import net.checkconsulting.assuranceiard.api.exception.PeriodeFacturationInconnueException;
import net.checkconsulting.assuranceiard.api.service.facture.FactureService;
import net.checkconsulting.assuranceiard.api.dto.ContratResponse;
import net.checkconsulting.assuranceiard.api.dto.ContratResumeResponse;
import net.checkconsulting.assuranceiard.api.dto.EnvoyerCodeSignatureResponse;
import net.checkconsulting.assuranceiard.api.dto.FicheContratResponse;
import net.checkconsulting.assuranceiard.api.exception.ContratInconnuException;
import net.checkconsulting.assuranceiard.api.exception.CodeSignatureEpuiseException;
import net.checkconsulting.assuranceiard.api.exception.CodeSignatureExpireException;
import net.checkconsulting.assuranceiard.api.exception.CodeSignatureIncorrectException;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.exception.GestionErreurs;
import net.checkconsulting.assuranceiard.api.exception.MandatNonSigneException;
import net.checkconsulting.assuranceiard.api.exception.SessionInvalideException;
import net.checkconsulting.assuranceiard.api.service.ContratService;
import net.checkconsulting.assuranceiard.api.service.EspaceClientService;
import net.checkconsulting.assuranceiard.api.service.document.RegenerationContratService;
import net.checkconsulting.assuranceiard.api.dto.RegenerationResponse;
import net.checkconsulting.assuranceiard.api.exception.RegenerationImpossibleException;
import net.checkconsulting.assuranceiard.api.exception.RegenerationInutileException;
import net.checkconsulting.assuranceiard.api.exception.RegenerationTropRapprocheeException;
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

    @MockitoBean
    private EspaceClientService espaceClientService;

    @MockitoBean
    private RegenerationContratService regenerationService;

    @MockitoBean
    private FactureService factureService;

    private ResultActions genererFacture(String corps) throws Exception {
        return mockMvc.perform(post("/contrats/AE-2026-000001/factures")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, JETON)
                .content(corps));
    }

    @Test
    void us22GenereLaFactureDUneEcheance() throws Exception {
        DemandeFactureRequest demande = new DemandeFactureRequest(DemandeFactureRequest.Periode.ECHEANCE,
                LocalDate.of(2026, 11, 10));
        when(factureService.generer("AE-2026-000001", demande)).thenReturn(new FactureGenereeResponse(
                new MesFacturesResponse.Facture("FAC-2026-000042", LocalDate.of(2026, 11, 10),
                        LocalDate.of(2026, 12, 9), new BigDecimal("16.97"), new BigDecimal("1.53"),
                        new BigDecimal("18.50"), Instant.parse("2026-11-25T10:00:00Z"), 7L),
                true, false));

        genererFacture("{\"periode\":\"ECHEANCE\",\"date\":\"2026-11-10\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facture.numero").value("FAC-2026-000042"))
                .andExpect(jsonPath("$.facture.documentId").value(7))
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.existante").value(false));
    }

    @Test
    void us22UneDemandeDeFactureIncompleteEstRefusee() throws Exception {
        genererFacture("{\"periode\":\"ECHEANCE\"}").andExpect(status().isBadRequest());
        verifyNoInteractions(factureService);
    }

    @Test
    void us22PeriodeInconnue404() throws Exception {
        when(factureService.generer(eq("AE-2026-000001"), any())).thenThrow(new PeriodeFacturationInconnueException());

        genererFacture("{\"periode\":\"ANNEE\",\"date\":\"2026-01-01\"}").andExpect(status().isNotFound());
    }

    @Test
    void us22FactureImpossible422() throws Exception {
        when(factureService.generer(eq("AE-2026-000001"), any())).thenThrow(new FactureImpossibleException());

        genererFacture("{\"periode\":\"ANNEE\",\"date\":\"2026-01-01\"}")
                .andExpect(status().isUnprocessableContent());
    }

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

    private ResultActions envoyerCode(UUID reference) throws Exception {
        return mockMvc.perform(post("/devis/contrat/" + reference + "/signature/code")
                .header(HttpHeaders.AUTHORIZATION, JETON));
    }

    private ResultActions renvoyerCode(UUID reference) throws Exception {
        return mockMvc.perform(post("/devis/contrat/" + reference + "/signature/code/renvoyer")
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
    void envoyerCodeRetourneLEmailMasque() throws Exception {
        when(contratService.envoyerCode(JETON, REFERENCE))
                .thenReturn(new EnvoyerCodeSignatureResponse("j***@example.com"));

        envoyerCode(REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailMasque").value("j***@example.com"));
    }

    @Test
    void renvoyerCodeRetourneLEmailMasque() throws Exception {
        when(contratService.renvoyerCode(JETON, REFERENCE))
                .thenReturn(new EnvoyerCodeSignatureResponse("j***@example.com"));

        renvoyerCode(REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailMasque").value("j***@example.com"));
    }

    @Test
    void signeLeContrat() throws Exception {
        ContratResponse signee = new ContratResponse(
                "AE-2026-000001", "ACTIF", "M.", "Jean", "Dupont", "0612345678",
                "12 rue des Lilas", "75001", "Paris", "appartement", 45,
                "confort", List.of("assistance-depannage"), 300,
                new BigDecimal("18.50"), new BigDecimal("222.00"), 10,
                "7890", "AE-2026-ABCDEF12",
                LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 1),
                false, LocalDate.of(2026, 11, 5));
        when(contratService.signer(eq(JETON), eq(REFERENCE), any())).thenReturn(signee);

        signer(REFERENCE, "{\"code\":\"123456\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ACTIF"));
    }

    @Test
    void retourne422SiCodeSignatureIncorrect() throws Exception {
        when(contratService.signer(eq(JETON), eq(REFERENCE), any()))
                .thenThrow(new CodeSignatureIncorrectException(2));

        signer(REFERENCE, "{\"code\":\"999999\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.essaisRestants").value(2));
    }

    @Test
    void retourne410SiCodeExpire() throws Exception {
        when(contratService.signer(eq(JETON), eq(REFERENCE), any()))
                .thenThrow(new CodeSignatureExpireException());

        signer(REFERENCE, "{\"code\":\"123456\"}")
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.motif").value("code-expire"));
    }

    @Test
    void retourne409SiEssaisEpuises() throws Exception {
        when(contratService.signer(eq(JETON), eq(REFERENCE), any()))
                .thenThrow(new CodeSignatureEpuiseException());

        signer(REFERENCE, "{\"code\":\"123456\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motif").value("essais-epuises"));
    }

    @Test
    void us04EtUs18ListeMesContratsHabitationEtAuto() throws Exception {
        when(espaceClientService.contrats(JETON)).thenReturn(List.of(
                new ContratResumeResponse(REFERENCE, "AE-2026-000001", "HABITATION", "ACTIF", "confort",
                        "12 rue des Lilas, 75001 Paris", "appartement", "Paris", new BigDecimal("18.50"),
                        LocalDate.of(2026, 12, 1), LocalDate.of(2027, 12, 1), null, "•••• 0189"),
                new ContratResumeResponse(null, "AE-RECETTE-A03", "AUTO", "ACTIF", "TR", "Renault Clio · AB-003-CD", null,
                        null, new BigDecimal("45.00"), LocalDate.of(2026, 9, 25), LocalDate.of(2027, 9, 25), null,
                        "•••• 7788")));

        mockMvc.perform(get("/contrats").header(HttpHeaders.AUTHORIZATION, JETON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("AE-2026-000001"))
                .andExpect(jsonPath("$[1].produit").value("AUTO"))
                .andExpect(jsonPath("$[1].referenceDevis").doesNotExist())
                .andExpect(jsonPath("$[1].bienAssure").value("Renault Clio · AB-003-CD"));
    }

    @Test
    void us18FicheContratParNumero() throws Exception {
        when(espaceClientService.fiche("AE-RECETTE-A03")).thenReturn(new FicheContratResponse("AE-RECETTE-A03", null,
                "AUTO", "ACTIF", "TR", null,
                new FicheContratResponse.Vehicule("Renault", "Clio", "V TCe 90", "AB-003-CD"),
                new BigDecimal("500.00"), List.of(new FicheContratResponse.Garantie("RC", new BigDecimal("12.00"))),
                new BigDecimal("45.00"), new BigDecimal("540.00"), LocalDate.of(2026, 9, 25),
                LocalDate.of(2027, 9, 25), null,
                new FicheContratResponse.Paiement("MENSUEL", 5, "•••• 7788", "AE-RUM-RECETTE-A"),
                List.of(new FicheContratResponse.Document(12L, "CONDITIONS_PARTICULIERES",
                        "Conditions particulières", 1, null)),
                List.of(new FicheContratResponse.Version(12L, 1, "EN_VIGUEUR", null, "SIGNATURE", null, true)), true));

        mockMvc.perform(get("/contrats/AE-RECETTE-A03").header(HttpHeaders.AUTHORIZATION, JETON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicule.immatriculation").value("AB-003-CD"))
                .andExpect(jsonPath("$.paiement.ibanMasque").value("•••• 7788"))
                .andExpect(jsonPath("$.documents[0].id").value(12))
                .andExpect(jsonPath("$.versionsConditionsParticulieres[0].statut").value("EN_VIGUEUR"));
    }

    @Test
    void us18FicheDUnContratInconnu() throws Exception {
        when(espaceClientService.fiche("AE-INCONNU")).thenThrow(new ContratInconnuException());

        mockMvc.perform(get("/contrats/AE-INCONNU").header(HttpHeaders.AUTHORIZATION, JETON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Contrat inconnu."));
    }

    private ResultActions regenerer(String numero) throws Exception {
        return mockMvc.perform(post("/contrats/" + numero + "/conditions-particulieres")
                .header(HttpHeaders.AUTHORIZATION, JETON));
    }

    @Test
    void us21RegenereLesConditionsParticulieres() throws Exception {
        when(regenerationService.regenererALaDemandeDuClient("AE-RECETTE-A03"))
                .thenReturn(new RegenerationResponse(55L, 3, java.time.Instant.parse("2026-11-24T10:00:00Z"), true,
                        true));

        regenerer("AE-RECETTE-A03")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(55))
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.emailEnvoye").value(true));
    }

    @Test
    void us21Rg2StatutQuiNePermetPasLaRegenerationEst422() throws Exception {
        when(regenerationService.regenererALaDemandeDuClient("AE-RECETTE-A07")).thenThrow(new RegenerationImpossibleException());

        regenerer("AE-RECETTE-A07")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("pas possible")));
    }

    @Test
    void us21UneRegenerationParMinuteEst429() throws Exception {
        when(regenerationService.regenererALaDemandeDuClient("AE-RECETTE-A03")).thenThrow(new RegenerationTropRapprocheeException());

        regenerer("AE-RECETTE-A03")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("une minute")));
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
                "AE-2026-ABCDEF12",
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 1),
                false,
                LocalDate.of(2026, 11, 5));
    }
}
