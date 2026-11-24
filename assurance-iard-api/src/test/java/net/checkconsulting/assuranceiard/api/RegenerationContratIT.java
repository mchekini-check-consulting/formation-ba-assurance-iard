package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import net.checkconsulting.assuranceiard.api.model.AccesRefuse;
import net.checkconsulting.assuranceiard.api.repository.AccesRefuseRepository;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.securite.MotifRefus;
import net.checkconsulting.assuranceiard.api.service.EnvoiConditionsParticulieres;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * US-21 sur une vraie base, avec les données de recette T-05 : Camille (contrats auto, un par statut) régénère ses
 * conditions particulières ; Alice (habitation) ne peut pas régénérer celles de Camille.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
    "assurance.donnees-test.actif=true",
    "assurance.justificatifs.dossier=target/justificatifs-regeneration"
})
@AutoConfigureMockMvc
class RegenerationContratIT {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String ALICE = "recette.assure1@assureasy.fr";
    private static final String CAMILLE = "recette.auto@assureasy.fr";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CompteRepository comptes;

    @Autowired
    private AccesRefuseRepository journal;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private TransactionTemplate transaction;

    @MockitoBean
    private EnvoiConditionsParticulieres envoi;

    private String alice;
    private String camille;

    @BeforeEach
    void connecter() {
        alice = jeton(ALICE);
        camille = jeton(CAMILLE);
    }

    private String jeton(String email) {
        return transaction.execute(etat ->
                "Bearer " + sessionService.ouvrir(comptes.findByEmail(email).orElseThrow()).jeton());
    }

    private ResultActions regenerer(String jeton, String numero) throws Exception {
        return mockMvc.perform(post("/contrats/{numero}/conditions-particulieres", numero)
                .header(HttpHeaders.AUTHORIZATION, jeton));
    }

    private List<AccesRefuse> refusDe(String email) {
        long compte = transaction.execute(etat -> comptes.findByEmail(email).orElseThrow().getId());
        return journal.findAll(Sort.by("id")).stream()
                .filter(ligne -> Long.valueOf(compte).equals(ligne.getCompteId()))
                .toList();
    }

    @Test
    void ac1Ac2Ac3RegenereLaVersionSuivanteToutesLesVersionsRestentVisiblesEtLeClientRecoitLePdf() throws Exception {
        String reponse = regenerer(camille, "AE-RECETTE-A03")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.emailEnvoye").value(true))
                .andReturn().getResponse().getContentAsString();
        long v2 = Long.parseLong(reponse.replaceAll(".*\"documentId\":(\\d+).*", "$1"));

        // AC3 : PDF envoyé au client propriétaire, en pièce jointe.
        verify(envoi).envoyer(eq(CAMILLE), eq("Camille"), eq("AE-RECETTE-A03"), eq(2), any(), any());

        // AC2 : la nouvelle version est en vigueur, l'ancienne remplacée.
        mockMvc.perform(get("/contrats/{numero}", "AE-RECETTE-A03").header(HttpHeaders.AUTHORIZATION, camille))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionsConditionsParticulieres.length()").value(2))
                .andExpect(jsonPath("$.versionsConditionsParticulieres[0].version").value(2))
                .andExpect(jsonPath("$.versionsConditionsParticulieres[0].statut").value("EN_VIGUEUR"))
                .andExpect(jsonPath("$.versionsConditionsParticulieres[0].motif").value("DEMANDE_CLIENT"))
                .andExpect(jsonPath("$.versionsConditionsParticulieres[0].genereLe").isNotEmpty())
                .andExpect(jsonPath("$.versionsConditionsParticulieres[1].version").value(1))
                .andExpect(jsonPath("$.versionsConditionsParticulieres[1].statut").value("REMPLACE"))
                // Rien n'a changé depuis : le bouton n'est plus proposé.
                .andExpect(jsonPath("$.conditionsParticulieresAJour").value(true));

        // Téléchargement par la route de T-03 (empreinte vérifiée).
        byte[] pdf = mockMvc.perform(get("/documents-contrat/{id}", v2).header(HttpHeaders.AUTHORIZATION, camille))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        // La date d'effet du PDF régénéré est celle du contrat, jamais la date du jour.
        LocalDate dateEffet = jdbc.queryForObject(
                "SELECT date_effet FROM contrat WHERE numero = 'AE-RECETTE-A03'", LocalDate.class);
        assertThat(dateEffet).isNotEqualTo(LocalDate.now(PARIS));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(document))
                    .contains("Date d'effet : " + dateEffet.format(JJ_MM_AAAA))
                    .doesNotContain("Date d'effet : " + LocalDate.now(PARIS).format(JJ_MM_AAAA));
        }

        // Une seule régénération par minute.
        regenerer(camille, "AE-RECETTE-A03")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("une minute")));
    }

    @Test
    void sansChangementDeDonneesLaRegenerationEstRefuseePuisAutoriseeDesQuUneDonneeChange() throws Exception {
        // Contrat habitation de Camille (Avenant en attente), sans conditions particulières : la V1 est créée.
        regenerer(camille, "AE-RECETTE-H01").andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        // La dernière régénération date de plus d'une minute : seule la règle « données inchangées » joue.
        jdbc.update("""
                UPDATE document SET date_creation = date_creation - INTERVAL '2 minutes'
                WHERE contrat_id = (SELECT id FROM contrat WHERE numero = 'AE-RECETTE-H01')""");

        regenerer(camille, "AE-RECETTE-H01")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("déjà à jour")));

        // Une donnée imprimée dans les conditions particulières change (le prénom du souscripteur).
        jdbc.update("UPDATE compte SET prenom = 'Camille-Marie' WHERE email = ?", CAMILLE);
        try {
            mockMvc.perform(get("/contrats/{numero}", "AE-RECETTE-H01").header(HttpHeaders.AUTHORIZATION, camille))
                    .andExpect(jsonPath("$.conditionsParticulieresAJour").value(false));
            regenerer(camille, "AE-RECETTE-H01")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.version").value(2));
        } finally {
            jdbc.update("UPDATE compte SET prenom = 'Camille' WHERE email = ?", CAMILLE);
        }
    }

    @Test
    void rg1NeModifieNiLesGarantiesNiLaCotisation() throws Exception {
        String sql = """
                SELECT c.cotisation_mensuelle, c.cotisation_annuelle, c.franchise, c.statut,
                       (SELECT string_agg(o.code_option || ':' || o.prix_mensuel_ttc, ',' ORDER BY o.id)
                        FROM contrat_option o WHERE o.contrat_id = c.id) AS options
                FROM contrat c WHERE c.numero = 'AE-RECETTE-A04'""";
        Map<String, Object> avant = jdbc.queryForMap(sql);

        regenerer(camille, "AE-RECETTE-A04").andExpect(status().isOk());

        assertThat(jdbc.queryForMap(sql)).isEqualTo(avant);
    }

    @Test
    void rg2ImpossibleSurUnContratSuspendu() throws Exception {
        regenerer(camille, "AE-RECETTE-A07")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("pas possible")));

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM document d JOIN contrat c ON c.id = d.contrat_id
                WHERE c.numero = 'AE-RECETTE-A07' AND d.type = 'CONDITIONS_PARTICULIERES'""", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void us04UnClientNeRegenerePasLeContratDUnAutreEtLeRefusEstTrace() throws Exception {
        int avant = refusDe(ALICE).size();

        regenerer(alice, "AE-RECETTE-A05").andExpect(status().isForbidden());

        List<AccesRefuse> refus = refusDe(ALICE);
        assertThat(refus).hasSize(avant + 1);
        assertThat(refus.get(refus.size() - 1)).satisfies(ligne -> {
            assertThat(ligne.getMotif()).isEqualTo(MotifRefus.PAS_PROPRIETAIRE);
            assertThat(ligne.getIdentifiant()).isEqualTo("AE-RECETTE-A05");
        });
        assertThat(jdbc.queryForObject("""
                SELECT MAX(d.version) FROM document d JOIN contrat c ON c.id = d.contrat_id
                WHERE c.numero = 'AE-RECETTE-A05' AND d.type = 'CONDITIONS_PARTICULIERES'""", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void sansSessionLaRegenerationEst401() throws Exception {
        mockMvc.perform(post("/contrats/{numero}/conditions-particulieres", "AE-RECETTE-A03"))
                .andExpect(status().isUnauthorized());
    }
}
