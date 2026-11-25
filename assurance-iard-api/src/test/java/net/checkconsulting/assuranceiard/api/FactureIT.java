package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.checkconsulting.assuranceiard.api.model.AccesRefuse;
import net.checkconsulting.assuranceiard.api.repository.AccesRefuseRepository;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.securite.MotifRefus;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * US-22 sur une vraie base, avec les données de recette T-05 : Alice (habitation, échéances enregistrées et une
 * facture), Camille (contrats auto, un par statut, échéancier calculé) et un prospect sans contrat.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
    "assurance.donnees-test.actif=true",
    "assurance.justificatifs.dossier=target/justificatifs-factures"
})
@AutoConfigureMockMvc
class FactureIT {

    private static final String ALICE = "recette.assure1@assureasy.fr";
    private static final String CAMILLE = "recette.auto@assureasy.fr";
    private static final String PROSPECT = "recette.prospect@assureasy.fr";
    private static final String NUMERO_FACTURE = "FAC-\\d{4}-\\d{6}";

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

    private String mesFactures(String jeton) throws Exception {
        return mockMvc.perform(get("/espace-client/factures").header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private ResultActions generer(String jeton, String numero, String periode, String date) throws Exception {
        return mockMvc.perform(post("/contrats/{numero}/factures", numero)
                .header(HttpHeaders.AUTHORIZATION, jeton)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"periode\":\"" + periode + "\",\"date\":\"" + date + "\"}"));
    }

    private static String premiere(String json, String numeroContrat, String liste, String champ) {
        List<String> valeurs = JsonPath.read(json,
                "$.contrats[?(@.numero == '" + numeroContrat + "')]." + liste + "[0]." + champ);
        return valeurs.getFirst();
    }

    private static final String ALICE_CONTRAT = "AE-RECETTE-1";

    private static List<String> dates(String json, String filtre) {
        return JsonPath.read(json, "$.contrats[0].echeances[?(" + filtre + ")].date");
    }

    @Test
    void ac1Recette1EcheancierCompleteDuPlusRecentAuPlusAncienChaqueAnneeVaut12Mensualites() throws Exception {
        mockMvc.perform(get("/espace-client/factures").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aucuneCotisation").value(false))
                .andExpect(jsonPath("$.contrats[0].numero").value(ALICE_CONTRAT))
                // 4 échéances enregistrées (3 payées, 1 à venir) sur 2 années d'assurance, complétées par le calcul.
                .andExpect(jsonPath("$.contrats[0].echeances.length()").value(24))
                .andExpect(jsonPath("$.contrats[0].annees.length()").value(2))
                .andExpect(jsonPath("$.contrats[0].annees[0].montantTtc").value(222.00))
                .andExpect(jsonPath("$.contrats[0].annees[1].montantTtc").value(222.00))
                // Année en cours : pas encore réglée. Année précédente : entièrement réglée, facturable.
                .andExpect(jsonPath("$.contrats[0].annees[0].reglee").value(false))
                .andExpect(jsonPath("$.contrats[0].annees[0].facturable").value(false))
                .andExpect(jsonPath("$.contrats[0].annees[1].reglee").value(true))
                .andExpect(jsonPath("$.contrats[0].annees[1].facturable").value(true));

        String json = mesFactures(alice);
        List<String> toutes = JsonPath.read(json, "$.contrats[0].echeances[*].date");
        assertThat(toutes).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        List<String> numeros = JsonPath.read(json, "$.contrats[0].factures[*].numero");
        assertThat(numeros).contains("FAC-RECETTE-0001");
        // Recette : aucune échéance avant la date d'effet ; l'échéancier commence à la date d'effet.
        LocalDate effet = jdbc.queryForObject("SELECT date_effet FROM contrat WHERE numero = ?", LocalDate.class,
                ALICE_CONTRAT);
        assertThat(toutes).allMatch(date -> !LocalDate.parse(date).isBefore(effet));
        assertThat(JsonPath.<String>read(json, "$.contrats[0].annees[1].debut")).isEqualTo(effet.toString());
        // Recette point 2 : seules les échéances payées sont facturables.
        assertThat(dates(json, "@.facturable == true")).isEqualTo(dates(json, "@.statut == 'PAYEE'"));
    }

    @Test
    void ac1Ac2Ac3GenereUneEcheancePayeeStockeTelechargeEtSansDoublon() throws Exception {
        String date = dates(mesFactures(alice), "@.statut == 'PAYEE' && @.facture == null").getFirst();

        String reponse = generer(alice, ALICE_CONTRAT, "ECHEANCE", date)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existante").value(false))
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.facture.periodeDebut").value(date))
                .andExpect(jsonPath("$.facture.montantTtc").value(18.50))
                .andReturn().getResponse().getContentAsString();
        String numero = JsonPath.read(reponse, "$.facture.numero");
        Integer documentId = JsonPath.read(reponse, "$.facture.documentId");
        assertThat(numero).matches(NUMERO_FACTURE);

        // Document versionné de T-03 : type FACTURE, référence = numéro de facture.
        Map<String, Object> document = jdbc.queryForMap(
                "SELECT type, reference, stocke FROM document WHERE id = ?", documentId.longValue());
        assertThat(document).containsEntry("type", "FACTURE").containsEntry("reference", numero)
                .containsEntry("stocke", true);
        // HT + taxe = TTC (contrainte de la base, vérifiée aussi ici).
        Map<String, Object> facture = jdbc.queryForMap(
                "SELECT montant_ht + taxe = montant_ttc AS juste FROM facture WHERE numero = ?", numero);
        assertThat(facture).containsEntry("juste", true);

        // AC3 : contenu du PDF téléchargé.
        byte[] pdf = mockMvc.perform(get("/documents-contrat/{id}", documentId).header(HttpHeaders.AUTHORIZATION,
                        alice))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        String texte;
        try (PDDocument lu = Loader.loadPDF(pdf)) {
            texte = new PDFTextStripper().getText(lu).replaceAll("\\s+", " ");
        }
        assertThat(texte).contains("Facture n° " + numero, "Date de la facture", "Alice", "Martin",
                "Contrat n° " + ALICE_CONTRAT, "Période couverte", "Détail par garantie", "Prime hors taxe",
                "Taxe sur les conventions d'assurance (TSCA)", "Total TTC", "Exonéré de TVA (article 261 C du CGI)",
                "Assur'Easy SA");

        // Deuxième clic : la même facture, pas de nouveau numéro.
        generer(alice, ALICE_CONTRAT, "ECHEANCE", date)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existante").value(true))
                .andExpect(jsonPath("$.facture.numero").value(numero));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM facture WHERE numero = ?", Integer.class, numero))
                .isEqualTo(1);

        // AC2 : elle apparaît dans Mes factures, téléchargeable, et l'échéance l'indique.
        String json = mesFactures(alice);
        List<Integer> telechargeables = JsonPath.read(json,
                "$.contrats[0].factures[?(@.numero == '" + numero + "')].documentId");
        assertThat(telechargeables).containsExactly(documentId);
        assertThat(dates(json, "@.facture == '" + numero + "'")).containsExactly(date);
    }

    @Test
    void recetteLaFactureDeLAnneeRegleeVautLaCotisationAnnuelle() throws Exception {
        String json = mesFactures(alice);
        String debut = JsonPath.read(json, "$.contrats[0].annees[1].debut");
        String fin = JsonPath.read(json, "$.contrats[0].annees[1].fin");

        generer(alice, ALICE_CONTRAT, "ANNEE", debut)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existante").value(false))
                .andExpect(jsonPath("$.facture.periodeDebut").value(debut))
                .andExpect(jsonPath("$.facture.periodeFin").value(fin))
                .andExpect(jsonPath("$.facture.montantTtc").value(222.00))
                .andExpect(jsonPath("$.facture.numero").value(org.hamcrest.Matchers.matchesRegex(NUMERO_FACTURE)));
        // L'année indique désormais sa facture.
        mockMvc.perform(get("/espace-client/factures").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(jsonPath("$.contrats[0].annees[1].facture").isNotEmpty());
    }

    @Test
    void recettePoint3LEcheanceCouverteParFacRecette0001RenvoieLaFactureExistante() throws Exception {
        String json = mesFactures(alice);
        List<String> couvertes = dates(json, "@.facture == 'FAC-RECETTE-0001'");
        assertThat(couvertes).hasSize(1);
        String compter = "SELECT COUNT(*) FROM facture f JOIN contrat c ON c.id = f.contrat_id WHERE c.numero = ?";
        Integer avant = jdbc.queryForObject(compter, Integer.class, ALICE_CONTRAT);

        generer(alice, ALICE_CONTRAT, "ECHEANCE", couvertes.getFirst())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existante").value(true))
                .andExpect(jsonPath("$.facture.numero").value("FAC-RECETTE-0001"));
        assertThat(jdbc.queryForObject(compter, Integer.class, ALICE_CONTRAT)).isEqualTo(avant);
    }

    @Test
    void recettePoint2PasDeFacturePourUneEcheanceAVenirNiPourUneAnneeNonReglee() throws Exception {
        String json = mesFactures(alice);

        generer(alice, ALICE_CONTRAT, "ECHEANCE", dates(json, "@.statut == 'A_VENIR'").getFirst())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("La facture d'une échéance est disponible une fois l'échéance payée."));
        generer(alice, ALICE_CONTRAT, "ANNEE", premiere(json, ALICE_CONTRAT, "annees", "debut"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Facture annuelle disponible quand l'année sera entièrement réglée."));
    }

    @Test
    void pasDeFacturePourUnContratEnAttenteDeSignatureNiPourUneDateInconnue() throws Exception {
        String json = mesFactures(camille);
        // Le contrat en attente de signature n'apparaît pas dans Mes factures.
        List<Object> enAttente = JsonPath.read(json, "$.contrats[?(@.numero == 'AE-RECETTE-A01')]");
        assertThat(enAttente).isEmpty();

        generer(camille, "AE-RECETTE-A01", "ECHEANCE", "2026-01-05").andExpect(status().isUnprocessableContent());
        generer(camille, "AE-RECETTE-A03", "ECHEANCE", "2000-01-01").andExpect(status().isNotFound());
        generer(camille, "AE-RECETTE-A03", "MOIS", "2026-01-05").andExpect(status().isBadRequest());
    }

    @Test
    void us04UnClientNeGenerePasLaFactureDUnAutreEtLeRefusEstTrace() throws Exception {
        String date = premiere(mesFactures(camille), "AE-RECETTE-A05", "echeances", "date");
        long compteAlice = transaction.execute(etat -> comptes.findByEmail(ALICE).orElseThrow().getId());
        long avant = journal.count();

        generer(alice, "AE-RECETTE-A05", "ECHEANCE", date).andExpect(status().isForbidden());

        List<AccesRefuse> refus = journal.findAll(Sort.by("id")).stream()
                .filter(ligne -> Long.valueOf(compteAlice).equals(ligne.getCompteId()))
                .toList();
        assertThat(journal.count()).isGreaterThan(avant);
        assertThat(refus.getLast().getMotif()).isEqualTo(MotifRefus.PAS_PROPRIETAIRE);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM facture f JOIN contrat c ON c.id = f.contrat_id WHERE c.numero = 'AE-RECETTE-A05'",
                Integer.class)).isZero();
    }

    @Test
    void ac4ProspectSansContratAucuneCotisation() throws Exception {
        mockMvc.perform(get("/espace-client/factures").header(HttpHeaders.AUTHORIZATION, jeton(PROSPECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contrats").isEmpty())
                .andExpect(jsonPath("$.aucuneCotisation").value(true));
    }
}
