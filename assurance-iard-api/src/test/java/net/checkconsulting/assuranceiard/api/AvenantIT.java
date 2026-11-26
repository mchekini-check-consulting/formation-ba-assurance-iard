package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.client.MoteurTarifaireClient;
import net.checkconsulting.assuranceiard.api.dto.ErreurChampMoteur;
import net.checkconsulting.assuranceiard.api.dto.TarifAutoResponse;
import net.checkconsulting.assuranceiard.api.dto.TarifRequest;
import net.checkconsulting.assuranceiard.api.dto.TarifResponse;
import net.checkconsulting.assuranceiard.api.exception.MoteurDonneesInvalidesException;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.service.EnvoiAvenant;
import net.checkconsulting.assuranceiard.api.service.EnvoiConditionsParticulieres;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import net.checkconsulting.assuranceiard.api.service.facture.EcheancierService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * US-23 sur une vraie base, avec les données de recette de Camille (T-05 corrigées par le script US-23) : modifier
 * ses garanties auto (AE-RECETTE-A03) et habitation (AE-RECETTE-H01), accepter ou abandonner l'avenant. Le moteur
 * tarifaire est simulé avec les prix des grilles AUTO-2026.1 et 2026.1 ; les e-mails sont simulés.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
    "assurance.donnees-test.actif=true",
    "assurance.justificatifs.dossier=target/justificatifs-avenant"
})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AvenantIT {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String CAMILLE = "recette.auto@assureasy.fr";
    private static final String ALICE = "recette.assure1@assureasy.fr";
    /** Grille AUTO-2026.1 (après V9 du moteur) : toutes compatibles avec Tous risques. */
    private static final List<TarifAutoResponse.PrixOption> OPTIONS_AUTO = List.of(
            new TarifAutoResponse.PrixOption("ASSISTANCE_0KM", new BigDecimal("3.50")),
            new TarifAutoResponse.PrixOption("VEHICULE_REMPLACEMENT", new BigDecimal("4.00")),
            new TarifAutoResponse.PrixOption("PROTECTION_CONDUCTEUR", new BigDecimal("5.00")),
            new TarifAutoResponse.PrixOption("PROTECTION_JURIDIQUE", new BigDecimal("2.50")));
    /** Grille habitation 2026.1 (sprint 1), codes du moteur. */
    private static final List<TarifResponse.PrixOption> OPTIONS_HABITATION = List.of(
            new TarifResponse.PrixOption("OBJETS_VALEUR", new BigDecimal("2.50")),
            new TarifResponse.PrixOption("ASSISTANCE", new BigDecimal("1.50")),
            new TarifResponse.PrixOption("PROTECTION_JURIDIQUE", new BigDecimal("2.90")));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CompteRepository comptes;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private EcheancierService echeancier;

    @Autowired
    private DonneesContratRepository donneesContrat;

    @MockitoBean
    private MoteurTarifaireClient moteur;

    @MockitoBean
    private EnvoiAvenant envoiAvenant;

    @MockitoBean
    private EnvoiConditionsParticulieres envoiConditionsParticulieres;

    private String camille;

    @BeforeEach
    void preparer() {
        camille = jeton(CAMILLE);
        when(moteur.calculerAuto(any())).thenAnswer(appel -> {
            MoteurTarifaireClient.DemandeMoteurAuto demande = appel.getArgument(0);
            TarifAutoResponse.Detail detail = demande.options() == null ? null
                    : new TarifAutoResponse.Detail(demande.formule(), new BigDecimal("30.00"),
                            OPTIONS_AUTO.stream().filter(option -> demande.options().contains(option.option())).toList(),
                            BigDecimal.ZERO, BigDecimal.ZERO);
            return new TarifAutoResponse(UUID.randomUUID(), "AUTO-2026.1", true, "TR", List.of(), OPTIONS_AUTO, detail,
                    LocalDate.now(), null, null, null);
        });
        when(moteur.calculer(any())).thenAnswer(appel -> {
            TarifRequest demande = appel.getArgument(0);
            // Recette US-23 (KO « champs invalides ») : même règle que le moteur (ChampsConditionnelsValidator).
            if ("APPARTEMENT".equals(demande.typeLogement()) && demande.etage() == null
                    || "MAISON".equals(demande.typeLogement()) && demande.dependances() == null) {
                throw new MoteurDonneesInvalidesException(List.of(new ErreurChampMoteur("etage", "obligatoire")));
            }
            return new TarifResponse(UUID.randomUUID(), "2026.1", true, null, null, null,
                    new TarifResponse.Detail("CONFORT", new BigDecimal("13.19"), OPTIONS_HABITATION, BigDecimal.ZERO,
                            new BigDecimal("20.09"), new BigDecimal("241.08")),
                    null, null, null, null, null, null);
        });
    }

    private String jeton(String email) {
        return transaction.execute(etat ->
                "Bearer " + sessionService.ouvrir(comptes.findByEmail(email).orElseThrow()).jeton());
    }

    private ResultActions garanties(String jeton, String numero) throws Exception {
        return mockMvc.perform(get("/contrats/{numero}/garanties", numero).header(HttpHeaders.AUTHORIZATION, jeton));
    }

    private ResultActions fiche(String numero) throws Exception {
        return mockMvc.perform(get("/contrats/{numero}", numero).header(HttpHeaders.AUTHORIZATION, camille));
    }

    private ResultActions preparer(String jeton, String numero, String garanties) throws Exception {
        return mockMvc.perform(post("/contrats/{numero}/avenants", numero).header(HttpHeaders.AUTHORIZATION, jeton)
                .contentType(MediaType.APPLICATION_JSON).content("{\"garanties\":[" + garanties + "]}"));
    }

    private ResultActions accepter(String numero, boolean accepte) throws Exception {
        return mockMvc.perform(post("/contrats/{numero}/avenants/en-attente/acceptation", numero)
                .header(HttpHeaders.AUTHORIZATION, camille).header("X-Forwarded-For", "198.51.100.23")
                .contentType(MediaType.APPLICATION_JSON).content("{\"accepte\":" + accepte + "}"));
    }

    private ResultActions abandonner(String numero) throws Exception {
        return mockMvc.perform(post("/contrats/{numero}/avenants/en-attente/abandon", numero)
                .header(HttpHeaders.AUTHORIZATION, camille));
    }

    private String texte(long documentId) throws Exception {
        byte[] pdf = mockMvc.perform(get("/documents-contrat/{id}", documentId).header(HttpHeaders.AUTHORIZATION,
                camille)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static LocalDate demain() {
        return LocalDate.now(PARIS).plusDays(1);
    }

    /**
     * RG3 : première échéance de l'échéancier d'US-22 à partir de la date d'effet (le 5 du mois, mandat de Camille, au
     * moins 14 jours après la signature : RG3/RG4 d'US-14).
     */
    private LocalDate premiereEcheanceApresLEffet(String numero) {
        long id = jdbc.queryForObject("SELECT id FROM contrat WHERE numero = ?", Long.class, numero);
        return echeancier.de("ACTIF", donneesContrat.lire(id).orElseThrow()).echeances().stream()
                .map(EcheancierService.LigneEcheance::date)
                .filter(date -> !date.isBefore(demain()))
                .min(java.util.Comparator.naturalOrder())
                .orElseThrow();
    }

    /** En premier : les autres tests modifient les contrats de Camille. */
    @Test
    @Order(1)
    void lesDonneesDeTestDeCamilleSontCoherentesAvecLesRegles() {
        Map<String, Object> h01 = jdbc.queryForMap("""
                SELECT c.statut, c.cotisation_mensuelle, a.statut AS statut_avenant,
                       a.date_effet = CURRENT_DATE + 1 AS effet_demain, a.prime_avant_ttc,
                       a.prime_apres_ttc, l.code_option, l.prix_mensuel_ttc, d.reference
                FROM contrat c JOIN avenant a ON a.contrat_id = c.id JOIN avenant_ligne l ON l.avenant_id = a.id
                LEFT JOIN document d ON d.id = a.document_id
                WHERE c.numero = 'AE-RECETTE-H01'""");
        assertThat(h01).containsEntry("statut", "AVENANT_EN_ATTENTE").containsEntry("statut_avenant", "EN_ATTENTE")
                .containsEntry("code_option", "objets-valeur");
        assertThat((BigDecimal) h01.get("prime_avant_ttc")).isEqualByComparingTo("18.50");
        assertThat((BigDecimal) h01.get("cotisation_mensuelle")).isEqualByComparingTo("18.50");
        // 18,50 + 2,50 (objets de valeur, grille du sprint 1) = 21,00.
        assertThat((BigDecimal) h01.get("prime_apres_ttc")).isEqualByComparingTo("21.00");
        assertThat(h01).containsEntry("effet_demain", true);
        assertThat((String) h01.get("reference")).startsWith("AV-");

        Map<String, Object> a03 = jdbc.queryForMap("""
                SELECT c.statut, c.cotisation_mensuelle, c.cotisation_annuelle,
                       (SELECT string_agg(o.code_option || ':' || o.prix_mensuel_ttc || ':' || o.obligatoire, ','
                                          ORDER BY o.id) FROM contrat_option o WHERE o.contrat_id = c.id) AS options
                FROM contrat c WHERE c.numero = 'AE-RECETTE-A03'""");
        assertThat(a03).containsEntry("statut", "ACTIF")
                .containsEntry("options", "RC:12.00:true,PROTECTION_JURIDIQUE:2.50:false");
        assertThat((BigDecimal) a03.get("cotisation_mensuelle")).isEqualByComparingTo("47.50");
        assertThat((BigDecimal) a03.get("cotisation_annuelle")).isEqualByComparingTo("570.00");

        // A05 (« Avenant en attente » dans T-05) : un vrai avenant EN_ATTENTE, 55,00 + 3,50 = 58,50 €.
        Map<String, Object> a05 = jdbc.queryForMap("""
                SELECT c.statut, c.cotisation_mensuelle, a.statut AS statut_avenant, a.prime_avant_ttc,
                       a.prime_apres_ttc, a.date_effet = CURRENT_DATE + 1 AS effet_demain, l.code_option, l.action,
                       l.prix_mensuel_ttc
                FROM contrat c JOIN avenant a ON a.contrat_id = c.id JOIN avenant_ligne l ON l.avenant_id = a.id
                WHERE c.numero = 'AE-RECETTE-A05'""");
        assertThat(a05).containsEntry("statut", "AVENANT_EN_ATTENTE").containsEntry("statut_avenant", "EN_ATTENTE")
                .containsEntry("effet_demain", true).containsEntry("code_option", "ASSISTANCE_0KM")
                .containsEntry("action", "AJOUT");
        assertThat((BigDecimal) a05.get("prime_avant_ttc")).isEqualByComparingTo("55.00")
                .isEqualByComparingTo((BigDecimal) a05.get("cotisation_mensuelle"));
        assertThat((BigDecimal) a05.get("prime_apres_ttc")).isEqualByComparingTo("58.50");
        // Aucun contrat « Avenant en attente » sans avenant EN_ATTENTE.
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM contrat c WHERE c.statut = 'AVENANT_EN_ATTENTE'
                AND NOT EXISTS (SELECT 1 FROM avenant a WHERE a.contrat_id = c.id AND a.statut = 'EN_ATTENTE')""",
                Integer.class)).isZero();

        // Migration V34 : l'assuré peut modifier ses contrats, pas le prospect.
        assertThat(jdbc.queryForObject("""
                SELECT valeur FROM permission WHERE profil = 'ASSURE' AND rubrique = 'CONTRATS' AND droit = 'MODIFIER'""",
                Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("""
                SELECT valeur FROM permission WHERE profil = 'PROSPECT' AND rubrique = 'CONTRATS' AND droit = 'MODIFIER'""",
                Boolean.class)).isFalse();
    }

    @Test
    void autoA03Ac1AAc4EtAc7AjoutEtRetraitPuisAcceptation() throws Exception {
        // AC1, AC2 : garanties actuelles et ajoutables, avec leur prix ; la RC n'est pas modifiable.
        garanties(camille, "AE-RECETTE-A03")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prenom").value("Camille"))
                .andExpect(jsonPath("$.cotisationMensuelle").value(47.5))
                .andExpect(jsonPath("$.garanties[*].code").value(org.hamcrest.Matchers.contains("RC",
                        "PROTECTION_JURIDIQUE", "ASSISTANCE_0KM", "VEHICULE_REMPLACEMENT", "PROTECTION_CONDUCTEUR")))
                .andExpect(jsonPath("$.garanties[0].souscrite").value(true))
                .andExpect(jsonPath("$.garanties[0].modifiable").value(false))
                .andExpect(jsonPath("$.garanties[0].raison").value(containsString("obligatoire")))
                .andExpect(jsonPath("$.garanties[1].prixMensuelTtc").value(2.5))
                .andExpect(jsonPath("$.garanties[2].prixMensuelTtc").value(3.5))
                .andExpect(jsonPath("$.avenantEnAttente").doesNotExist());

        // AC2 : retirer la RC est refusé avec la raison ; « Continuer » sans changement est refusé.
        preparer(camille, "AE-RECETTE-A03", "\"PROTECTION_JURIDIQUE\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("responsabilité civile est obligatoire")));
        preparer(camille, "AE-RECETTE-A03", "\"RC\", \"PROTECTION_JURIDIQUE\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Modifiez au moins une garantie pour continuer."));

        // AC3 : + assistance 0 km (3,50), − protection juridique (2,50) : 47,50 → 48,50 € par mois.
        LocalDate premiereEcheance = premiereEcheanceApresLEffet("AE-RECETTE-A03");
        preparer(camille, "AE-RECETTE-A03", "\"RC\", \"ASSISTANCE_0KM\"")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avenant.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.avenant.numero").value(startsWith("AV-")))
                .andExpect(jsonPath("$.avenant.ancienneCotisationMensuelle").value(47.5))
                .andExpect(jsonPath("$.avenant.nouvelleCotisationMensuelle").value(48.5))
                .andExpect(jsonPath("$.ancienneCotisationAnnuelle").value(570.0))
                .andExpect(jsonPath("$.nouvelleCotisationAnnuelle").value(582.0))
                .andExpect(jsonPath("$.avenant.dateEffet").value(demain().toString()))
                .andExpect(jsonPath("$.premiereEcheanceConcernee").value(premiereEcheance.toString()))
                // RG4 : la couverture diminue, le conseil est mis à jour avant l'acceptation.
                .andExpect(jsonPath("$.reductionCouverture").value(true))
                .andExpect(jsonPath("$.conseils[1]").value(containsString("Protection juridique")))
                .andExpect(jsonPath("$.ipidFormule").value("TR"));

        // AC5 : avenant en attente, contrat couvert aux conditions actuelles.
        fiche("AE-RECETTE-A03")
                .andExpect(jsonPath("$.statut").value("AVENANT_EN_ATTENTE"))
                .andExpect(jsonPath("$.cotisationMensuelle").value(47.5))
                .andExpect(jsonPath("$.garanties[*].code").value(hasItem("PROTECTION_JURIDIQUE")))
                .andExpect(jsonPath("$.avenants[0].statut").value("EN_ATTENTE"));

        // AC4 : case non cochée refusée (RG1), puis acceptation.
        accepter("AE-RECETTE-A03", false).andExpect(status().isBadRequest());
        String reponse = accepter("AE-RECETTE-A03", true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avenant.statut").value("ACCEPTE"))
                .andExpect(jsonPath("$.avenant.accepteLe").isNotEmpty())
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.emailEnvoye").value(true))
                .andExpect(jsonPath("$.versionConditionsParticulieres").value(2))
                .andReturn().getResponse().getContentAsString();
        String numeroAvenant = JsonPath.read(reponse, "$.avenant.numero");
        long documentAvenant = ((Number) JsonPath.read(reponse, "$.avenant.documentId")).longValue();

        // Acceptation horodatée et conservée ; garanties et cotisation à jour ; contrat de nouveau Actif.
        Map<String, Object> enBase = jdbc.queryForMap("""
                SELECT a.accepte_le, a.accepte_ip, a.date_effet, c.statut, c.cotisation_mensuelle,
                       c.cotisation_annuelle
                FROM avenant a JOIN contrat c ON c.id = a.contrat_id
                WHERE c.numero = 'AE-RECETTE-A03' AND a.statut = 'ACCEPTE'""");
        assertThat(enBase.get("accepte_le")).isNotNull();
        assertThat(enBase).containsEntry("accepte_ip", "198.51.100.23").containsEntry("statut", "ACTIF");
        assertThat(((java.sql.Date) enBase.get("date_effet")).toLocalDate()).isEqualTo(demain());
        assertThat((BigDecimal) enBase.get("cotisation_mensuelle")).isEqualByComparingTo("48.50");
        assertThat((BigDecimal) enBase.get("cotisation_annuelle")).isEqualByComparingTo("582.00");
        verify(envoiAvenant).envoyer(eq(CAMILLE), eq("Camille"), eq("AE-RECETTE-A03"), eq(numeroAvenant), any(),
                any());

        // AC4 et AC7 : nouvelles garanties, historique des avenants, CP V2 « En vigueur » (motif AVENANT), V1 remplacée.
        fiche("AE-RECETTE-A03")
                .andExpect(jsonPath("$.statut").value("ACTIF"))
                .andExpect(jsonPath("$.garanties[*].code").value(org.hamcrest.Matchers.contains("RC", "ASSISTANCE_0KM")))
                .andExpect(jsonPath("$.cotisationMensuelle").value(48.5))
                .andExpect(jsonPath("$.avenants[0].numero").value(numeroAvenant))
                .andExpect(jsonPath("$.avenants[0].statut").value("ACCEPTE"))
                .andExpect(jsonPath("$.avenants[0].ajouts[0].code").value("ASSISTANCE_0KM"))
                .andExpect(jsonPath("$.avenants[0].retraits[0].code").value("PROTECTION_JURIDIQUE"))
                .andExpect(jsonPath("$.versionsConditionsParticulieres[0].statut").value("EN_VIGUEUR"))
                .andExpect(jsonPath("$.versionsConditionsParticulieres[0].motif").value("AVENANT"))
                .andExpect(jsonPath("$.versionsConditionsParticulieres[1].statut").value("REMPLACE"))
                .andExpect(jsonPath("$.avenants[0].documentId").value((int) documentAvenant));
        // « Mes documents » : l'avenant accepté est listé avec les contrats du client.
        mockMvc.perform(get("/contrats").header(HttpHeaders.AUTHORIZATION, camille))
                .andExpect(jsonPath("$[?(@.numero == 'AE-RECETTE-A03')].avenants[0].numero").value(hasItem(numeroAvenant)))
                .andExpect(jsonPath("$[?(@.numero == 'AE-RECETTE-A03')].avenants[0].documentId")
                        .value(hasItem((int) documentAvenant)));

        // PDF de l'avenant : contenu obligatoire.
        String avenant = texte(documentAvenant);
        String aujourdhui = LocalDate.now(PARIS).format(JJ_MM_AAAA);
        assertThat(avenant).contains("Avenant n° " + numeroAvenant, "Contrat n° AE-RECETTE-A03",
                "Date d'effet : " + demain().format(JJ_MM_AAAA), "Assistance dès 0 km : + 3,50 €",
                "Protection juridique : - 2,50 €", "Ancienne cotisation : 47,50 €", "Nouvelle cotisation : 48,50 €",
                "Avenant accepté en ligne le " + aujourdhui);

        // AC7 : la date d'effet des nouvelles CP est celle du contrat, jamais la date du jour ; nouvelles garanties.
        LocalDate effetContrat = jdbc.queryForObject(
                "SELECT date_effet FROM contrat WHERE numero = 'AE-RECETTE-A03'", LocalDate.class);
        long cpV2 = jdbc.queryForObject("""
                SELECT d.id FROM document d JOIN contrat c ON c.id = d.contrat_id
                WHERE c.numero = 'AE-RECETTE-A03' AND d.type = 'CONDITIONS_PARTICULIERES' AND d.statut = 'EN_VIGUEUR'""",
                Long.class);
        assertThat(texte(cpV2)).contains("Date d'effet : " + effetContrat.format(JJ_MM_AAAA), "Assistance dès 0 km",
                "48,50 € par mois").doesNotContain("Date d'effet : " + aujourdhui, "Protection juridique");

        // RG3 : échéances à 48,50 € à partir de la date d'effet, 47,50 € avant.
        long contratId = jdbc.queryForObject("SELECT id FROM contrat WHERE numero = 'AE-RECETTE-A03'", Long.class);
        List<EcheancierService.LigneEcheance> lignes = echeancier.de("ACTIF",
                donneesContrat.lire(contratId).orElseThrow()).echeances();
        assertThat(lignes).filteredOn(ligne -> !ligne.date().isBefore(demain()))
                .isNotEmpty().allSatisfy(ligne -> assertThat(ligne.montantTtc()).isEqualByComparingTo("48.50"));
        assertThat(lignes).filteredOn(ligne -> ligne.date().isBefore(demain()))
                .allSatisfy(ligne -> assertThat(ligne.montantTtc()).isEqualByComparingTo("47.50"));
    }

    @Test
    void habitationH01Ac5AvenantEnAttenteRemplaceAbandonnePuisAccepte() throws Exception {
        // AC5 : l'avenant en attente de T-05, contrat couvert aux conditions actuelles.
        garanties(camille, "AE-RECETTE-H01")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("AVENANT_EN_ATTENTE"))
                .andExpect(jsonPath("$.garanties[*].code").value(org.hamcrest.Matchers.contains("objets-valeur",
                        "assistance-depannage", "protection-juridique")))
                .andExpect(jsonPath("$.garanties[*].prixMensuelTtc").value(org.hamcrest.Matchers.contains(2.5, 1.5,
                        2.9)))
                .andExpect(jsonPath("$.avenantEnAttente.avenant.nouvelleCotisationMensuelle").value(21.0))
                .andExpect(jsonPath("$.avenantEnAttente.avenant.ajouts[0].code").value("objets-valeur"))
                .andExpect(jsonPath("$.avenantEnAttente.avenant.dateEffet").value(demain().toString()));
        fiche("AE-RECETTE-H01").andExpect(jsonPath("$.cotisationMensuelle").value(18.5));
        int avenantsAvant = jdbc.queryForObject("SELECT COUNT(*) FROM avenant", Integer.class);

        // Un nouvel avenant remplace l'ancien : 18,50 + 2,50 + 2,90 = 23,90 €.
        preparer(camille, "AE-RECETTE-H01", "\"objets-valeur\", \"protection-juridique\"")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avenant.nouvelleCotisationMensuelle").value(23.9))
                .andExpect(jsonPath("$.reductionCouverture").value(false))
                .andExpect(jsonPath("$.ipidFormule").doesNotExist());
        assertThat(jdbc.queryForList("""
                SELECT a.statut FROM avenant a JOIN contrat c ON c.id = a.contrat_id
                WHERE c.numero = 'AE-RECETTE-H01' ORDER BY a.numero""", String.class))
                .containsExactly("ABANDONNE", "EN_ATTENTE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM avenant", Integer.class)).isEqualTo(avenantsAvant + 1);

        // Abandonner : ABANDONNE, rien n'est effacé ; le contrat redevient Actif aux conditions actuelles.
        abandonner("AE-RECETTE-H01").andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("ABANDONNE"));
        abandonner("AE-RECETTE-H01").andExpect(status().isNotFound());
        fiche("AE-RECETTE-H01")
                .andExpect(jsonPath("$.statut").value("ACTIF"))
                .andExpect(jsonPath("$.cotisationMensuelle").value(18.5))
                .andExpect(jsonPath("$.avenants.length()").value(2));

        // Puis accepter l'ajout des objets de valeur : 21,00 € par mois, 252,00 € par an.
        preparer(camille, "AE-RECETTE-H01", "\"objets-valeur\"").andExpect(status().isOk());
        accepter("AE-RECETTE-H01", true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avenant.nouvelleCotisationMensuelle").value(21.0));
        fiche("AE-RECETTE-H01")
                .andExpect(jsonPath("$.statut").value("ACTIF"))
                .andExpect(jsonPath("$.cotisationMensuelle").value(21.0))
                .andExpect(jsonPath("$.cotisationAnnuelle").value(252.0))
                .andExpect(jsonPath("$.garanties[0].code").value("objets-valeur"))
                .andExpect(jsonPath("$.garanties[0].prixMensuelTtc").value(2.5))
                .andExpect(jsonPath("$.versionsConditionsParticulieres[0].motif").value("AVENANT"))
                .andExpect(jsonPath("$.avenants.length()").value(3));
    }

    @Test
    void recetteA05RelireEtAccepterLAvenantEnAttenteDesDonneesDeTest() throws Exception {
        garanties(camille, "AE-RECETTE-A05")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avenantEnAttente.avenant.ancienneCotisationMensuelle").value(55.0))
                .andExpect(jsonPath("$.avenantEnAttente.avenant.nouvelleCotisationMensuelle").value(58.5))
                .andExpect(jsonPath("$.avenantEnAttente.nouvelleCotisationAnnuelle").value(702.0))
                .andExpect(jsonPath("$.avenantEnAttente.avenant.ajouts[0].libelle").value("Assistance dès 0 km"))
                .andExpect(jsonPath("$.avenantEnAttente.ipidFormule").value("INTER"));
        accepter("AE-RECETTE-A05", true).andExpect(status().isOk());
        fiche("AE-RECETTE-A05")
                .andExpect(jsonPath("$.statut").value("ACTIF"))
                .andExpect(jsonPath("$.cotisationMensuelle").value(58.5))
                .andExpect(jsonPath("$.garanties[*].code").value(org.hamcrest.Matchers.contains("RC",
                        "ASSISTANCE_0KM")));
    }

    @Test
    void ac6LesContratsQuiNeSontPasActifsSontRefusesParLeServeur() throws Exception {
        // A01 en attente de signature, A02 en attente d'effet, A04 justificatifs attendus, A06 impayé,
        // A07 suspendu, A08 résiliation en cours, A09 résilié, A10 renoncé.
        List<String> bloques = List.of("AE-RECETTE-A01", "AE-RECETTE-A02", "AE-RECETTE-A04", "AE-RECETTE-A06",
                "AE-RECETTE-A07", "AE-RECETTE-A08", "AE-RECETTE-A09", "AE-RECETTE-A10");
        for (String numero : bloques) {
            garanties(camille, numero).andExpect(status().is(422));
            preparer(camille, numero, "\"RC\", \"ASSISTANCE_0KM\"").andExpect(status().is(422));
        }
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM avenant a JOIN contrat c ON c.id = a.contrat_id
                WHERE c.numero = ANY (?)""", Integer.class, (Object) bloques.toArray(String[]::new))).isZero();
    }

    @Test
    void unAutreClientNeVoitNiNeModifieLesGarantiesDeCamille() throws Exception {
        String alice = jeton(ALICE);

        garanties(alice, "AE-RECETTE-A03").andExpect(status().isForbidden());
        preparer(alice, "AE-RECETTE-A03", "\"RC\", \"ASSISTANCE_0KM\"").andExpect(status().isForbidden());
    }
}
