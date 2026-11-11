package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.client.MoteurTarifaireClient;
import net.checkconsulting.assuranceiard.api.dto.TarifResponse;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DevisEnregistre;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisEnregistreRepository;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import net.checkconsulting.assuranceiard.api.service.ConservationDevisService;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** US-12 sur une vraie base. Chaque test crée ses propres comptes et devis, sans supposer une base vide. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class DevisEnregistreIT {

    @MockitoBean
    private MoteurTarifaireClient moteur;

    @BeforeEach
    void simulerLeMoteur() {
        when(moteur.calculer(any())).thenReturn(
                new TarifResponse(UUID.randomUUID(), "v", true, null, null, null,
                        new TarifResponse.Detail("CONFORT", null, List.of(), null,
                                new BigDecimal("14.72"), new BigDecimal("176.64")),
                        null, null, null, null, null, null));
    }

    private static final String CORPS = """
            {"reference":"%s",
            "logement":{"statut":"locataire","residence":"principale","typeLogement":"appartement",
            "adresse":"12 rue des Lilas","codePostal":"75001","ville":"Paris","surface":"60","nombrePieces":"3",
            "antecedent":"non-assure","dependances":"jardin"},
            "formule":"confort","options":["objets-valeur"],"franchise":"300"}
            """;
    private static final String CHEMIN_REPRISE = "/devis/enregistres/{ref}/reprise";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompteRepository comptes;

    @Autowired
    private RecommandationDevisRepository recommandations;

    @Autowired
    private DevisEnregistreRepository devisEnregistres;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ConservationDevisService conservation;

    private Compte compte;
    private String jeton;
    private UUID reference;

    @BeforeEach
    void preparer() {
        compte = nouveauCompte();
        jeton = jetonDe(compte);
        reference = nouvelleRecommandation();
    }

    private Compte nouveauCompte() {
        Compte nouveau = new Compte();
        nouveau.setEmail(UUID.randomUUID() + "@exemple.fr");
        nouveau.setMotDePasseHash("hash");
        return comptes.save(nouveau);
    }

    private String jetonDe(Compte proprietaire) {
        return "Bearer " + sessionService.ouvrir(proprietaire).jeton();
    }

    private UUID nouvelleRecommandation() {
        RecommandationDevis recommandation = new RecommandationDevis();
        recommandation.setReference(UUID.randomUUID());
        recommandation.setValeurContenu(15_000);
        recommandation.setObjetsValeur(true);
        recommandation.setRemboursement("vetuste");
        recommandation.setPriorite("prix");
        recommandation.setFormule("confort");
        recommandation.setRaisons(List.of("raison"));
        recommandation.setOptions(List.of("objets-valeur"));
        recommandation.setDateCreation(Instant.now());
        return recommandations.save(recommandation).getReference();
    }

    private void enregistrer() throws Exception {
        mockMvc.perform(post("/devis/enregistres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, jeton)
                        .content(CORPS.formatted(reference)))
                .andExpect(status().isOk());
    }

    private DevisEnregistre devisEnBase() {
        return devisEnregistres.findFirstByReferenceOrderByDateCreationDesc(reference).orElseThrow();
    }

    @Test
    void ac1EtAc3EnregistreEtAfficheDansMesDevis() throws Exception {
        enregistrer();

        LocalDate fin = LocalDate.now(ZoneId.of("Europe/Paris")).plusDays(30);
        mockMvc.perform(get("/devis/enregistres").header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reference").value(reference.toString()))
                .andExpect(jsonPath("$[0].prixMensuel").value(14.72))
                .andExpect(jsonPath("$[0].dateFinValidite").value(fin.toString()))
                .andExpect(jsonPath("$[0].expire").value(false));
    }

    @Test
    void enregistrerDeuxFoisNeCreePasDeDoublon() throws Exception {
        enregistrer();
        enregistrer();

        mockMvc.perform(get("/devis/enregistres").header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void ac5RepriseRendToutesLesReponses() throws Exception {
        enregistrer();

        mockMvc.perform(post(CHEMIN_REPRISE, reference).header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logement.adresse").value("12 rue des Lilas"))
                .andExpect(jsonPath("$.logement.surface").value("60"))
                .andExpect(jsonPath("$.logement.dependances").value("jardin"))
                .andExpect(jsonPath("$.besoins.valeurContenu").value(15_000))
                .andExpect(jsonPath("$.formule").value("confort"))
                .andExpect(jsonPath("$.options[0]").value("objets-valeur"))
                .andExpect(jsonPath("$.franchise").value("300"))
                .andExpect(jsonPath("$.prixMensuel").value(14.72))
                .andExpect(jsonPath("$.ancienPrixMensuel").isEmpty());
    }

    @Test
    void ac6UnDevisExpireEstRecalculeALaReprise() throws Exception {
        enregistrer();
        DevisEnregistre devis = devisEnBase();
        devis.setPrixMensuel(new BigDecimal("10.00"));
        devis.setDateFinValidite(LocalDate.now(ZoneId.of("Europe/Paris")).minusDays(1));
        devisEnregistres.save(devis);

        mockMvc.perform(get("/devis/enregistres").header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(jsonPath("$[0].expire").value(true));

        mockMvc.perform(post(CHEMIN_REPRISE, reference).header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ancienPrixMensuel").value(10.00))
                .andExpect(jsonPath("$.prixMensuel").value(14.72));

        mockMvc.perform(get("/devis/enregistres").header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(jsonPath("$[0].expire").value(false));
    }

    /** US-04, AC2 : le devis d'un autre est refusé (« Accès non autorisé »), plus seulement introuvable. */
    @Test
    void unClientNeVoitNiNeTouchePasAuxDevisDUnAutre() throws Exception {
        enregistrer();
        String autre = jetonDe(nouveauCompte());

        mockMvc.perform(get("/devis/enregistres").header(HttpHeaders.AUTHORIZATION, autre))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(post(CHEMIN_REPRISE, reference).header(HttpHeaders.AUTHORIZATION, autre))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/devis/enregistres/{ref}", reference).header(HttpHeaders.AUTHORIZATION, autre))
                .andExpect(status().isForbidden());

        assertThat(devisEnregistres.findFirstByReferenceOrderByDateCreationDesc(reference)).isPresent();
    }

    @Test
    void rg2EnregistrerExigeUnCompte() throws Exception {
        mockMvc.perform(post("/devis/enregistres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS.formatted(reference)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/devis/enregistres")).andExpect(status().isUnauthorized());
    }

    @Test
    void ac4EtRg2TelechargeUnVraiPdfSansCompte() throws Exception {
        byte[] pdf = mockMvc.perform(post("/devis/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS.formatted(reference)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(document))
                    .contains("12 rue des Lilas")
                    .contains("Confort")
                    .contains("Objets de valeur")
                    .contains("14,72 € par mois")
                    .contains("176,64 € par an");
        }
    }

    @Test
    void rgpdLeClientPeutSupprimerSonDevis() throws Exception {
        enregistrer();

        mockMvc.perform(delete("/devis/enregistres/{ref}", reference).header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isNoContent());

        assertThat(devisEnregistres.findFirstByReferenceOrderByDateCreationDesc(reference)).isEmpty();
    }

    @Test
    void rgpdSupprimeLesDevisTroisAnsApresLeDernierContact() throws Exception {
        enregistrer();
        UUID recent = reference;
        reference = nouvelleRecommandation();
        enregistrer();
        DevisEnregistre ancien = devisEnBase();
        ancien.setDateDernierContact(Instant.now().minus(1100, ChronoUnit.DAYS));
        devisEnregistres.save(ancien);

        conservation.supprimerDevisTropAnciens();

        assertThat(devisEnregistres.findFirstByReferenceOrderByDateCreationDesc(reference)).isEmpty();
        assertThat(devisEnregistres.findFirstByReferenceOrderByDateCreationDesc(recent)).isPresent();
    }
}
