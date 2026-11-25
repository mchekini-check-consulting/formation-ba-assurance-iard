package net.checkconsulting.assuranceiard.api.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import net.checkconsulting.assuranceiard.api.client.siv.SivClient;
import net.checkconsulting.assuranceiard.api.client.siv.SivReponse;
import net.checkconsulting.assuranceiard.api.client.siv.VehiculeInfoSiv;
import net.checkconsulting.assuranceiard.api.dto.CriteresEligibiliteResponse;
import net.checkconsulting.assuranceiard.api.dto.FormuleAutoResponse;
import net.checkconsulting.assuranceiard.api.dto.MarqueResponse;
import net.checkconsulting.assuranceiard.api.dto.ModeleResponse;
import net.checkconsulting.assuranceiard.api.dto.PrixFormuleAutoResponse;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.service.BesoinsAutoService;
import net.checkconsulting.assuranceiard.api.service.DevisAutoCreationService;
import net.checkconsulting.assuranceiard.api.service.DocumentService;
import net.checkconsulting.assuranceiard.api.service.EligibiliteAutoService;
import net.checkconsulting.assuranceiard.api.service.FormuleAutoService;
import net.checkconsulting.assuranceiard.api.service.ReferentielVehiculeService;
import net.checkconsulting.assuranceiard.api.service.TarificateurAutoService;
import net.checkconsulting.assuranceiard.api.service.document.DocumentsGeneresService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DevisAutoResource.class)
class DevisAutoResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SivClient sivClient;

    @MockitoBean
    private ReferentielVehiculeService referentielVehiculeService;

    @MockitoBean
    private EligibiliteAutoService eligibiliteAutoService;

    @MockitoBean
    private TarificateurAutoService tarificateurAutoService;

    @MockitoBean
    private DevisAutoCreationService devisAutoCreationService;

    @MockitoBean
    private BesoinsAutoService besoinsAutoService;

    @MockitoBean
    private FormuleAutoService formuleAutoService;

    @MockitoBean
    private DocumentsGeneresService documentsGeneresService;

    @Test
    void laRechercheParPlaqueTrouveLeVehicule() throws Exception {
        VehiculeInfoSiv info = new VehiculeInfoSiv(1L, "Peugeot", "308", "ESSENCE", "1.2", 2021, new BigDecimal("18500"));
        when(sivClient.consulterPlaque("AB-123-CD")).thenReturn(new SivReponse(SivReponse.TROUVE, info));

        mockMvc.perform(post("/v1/devis/auto/recherche-vehicule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"immatriculation\":\"AB-123-CD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("TROUVE"))
                .andExpect(jsonPath("$.marque").value("Peugeot"))
                .andExpect(jsonPath("$.modele").value("308"));
    }

    @Test
    void laRechercheParPlaqueInconnueRenvoieInconnu() throws Exception {
        when(sivClient.consulterPlaque(anyString())).thenReturn(new SivReponse(SivReponse.INCONNU, null));

        mockMvc.perform(post("/v1/devis/auto/recherche-vehicule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"immatriculation\":\"XX-000-XX\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("INCONNU"))
                .andExpect(jsonPath("$.marque").doesNotExist());
    }

    @Test
    void unePlaqueVideEst400() throws Exception {
        mockMvc.perform(post("/v1/devis/auto/recherche-vehicule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"immatriculation\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void leReferentielVehiculesEstRetourne() throws Exception {
        ModeleResponse modele = new ModeleResponse(1L, "308", "ESSENCE", "1.2", 2021, new BigDecimal("18500"));
        when(referentielVehiculeService.parMarque()).thenReturn(List.of(new MarqueResponse("Peugeot", List.of(modele))));

        mockMvc.perform(get("/v1/devis/auto/referentiels/vehicules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].marque").value("Peugeot"))
                .andExpect(jsonPath("$[0].modeles[0].modele").value("308"));
    }

    @Test
    void lesCriteresEligibiliteSontRetournes() throws Exception {
        when(eligibiliteAutoService.criteres()).thenReturn(
                new CriteresEligibiliteResponse(new BigDecimal("1.50"), "NON_PAIEMENT"));

        mockMvc.perform(get("/v1/devis/auto/referentiels/criteres-eligibilite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bonusMalusMaxLigne").value(1.5))
                .andExpect(jsonPath("$.resiliationBloquante").value("NON_PAIEMENT"));
    }

    @ParameterizedTest
    @CsvSource({
        "AB123CD,  AB-123-CD",
        "ab-123-cd, AB-123-CD",
        "AB-123-CD, AB-123-CD",
        "GH789JK,   GH-789-JK"
    })
    void laNormalisationDePlaqueFonctionne(String saisie, String attendu) {
        assertThat(DevisAutoResource.normaliserPlaque(saisie.strip())).isEqualTo(attendu.strip());
    }

    @Test
    void laFicheConseilTelechargeUnPdf() throws Exception {
        Document doc = mock(Document.class);
        when(doc.getId()).thenReturn(99L);
        when(documentsGeneresService.generer(any())).thenReturn(doc);
        when(documentsGeneresService.telecharger(99L)).thenReturn(
                new DocumentService.Fichier("fiche-conseil-1.pdf", "pdf", new byte[] {7}));

        mockMvc.perform(get("/v1/devis/auto/1/fiche-conseil"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType())
                        .startsWith("application/pdf"));
    }

    @Test
    void unePlaqueNonSivNestPasModifiee() {
        assertThat(DevisAutoResource.normaliserPlaque("ANCIENNE-PLAQUE-7700-WW-69")).isNotBlank();
    }

    @Test
    void lesPrixFormulesSontRetournes() throws Exception {
        when(formuleAutoService.obtenirPrix(1L)).thenReturn(new PrixFormuleAutoResponse(
                null, "AUTO-2026.1", null, List.of(), List.of()));

        mockMvc.perform(get("/v1/devis/auto/1/prix-formules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionGrille").value("AUTO-2026.1"));
    }

    @Test
    void laFormuleChoisieEstEnregistree() throws Exception {
        when(formuleAutoService.choisirFormule(any(), any())).thenReturn(
                new FormuleAutoResponse("INTER", List.of(), new BigDecimal("33.90"), new BigDecimal("406.80"), null));

        mockMvc.perform(put("/v1/devis/auto/1/formule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formuleChoisie\":\"INTER\",\"optionsChoisies\":[],\"choixDifferentDuConseil\":false,"
                                + "\"versionGrille\":\"AUTO-2026.1\",\"prixMensuelTtc\":null,\"prixAnnuelTtc\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formuleChoisie").value("INTER"));
    }
}
