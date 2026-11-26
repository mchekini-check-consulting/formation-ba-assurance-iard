package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.service.EnvoiSinistre;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import net.checkconsulting.assuranceiard.api.service.sinistre.PiecesJointesSinistreService;
import net.checkconsulting.assuranceiard.api.service.sinistre.PurgeBrouillonsSinistre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * US-24 sur une vraie base, avec les données de recette de Camille (auto) et d'Alice (habitation), corrigées par le
 * script US-24 : contrats ouverts à la déclaration, couverture (vol non couvert en formule Tiers), brouillon et purge,
 * pièces stockées en base, envoi, e-mails (simulés : on vérifie leur contenu) et contrôle d'accès.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
    "assurance.donnees-test.actif=true",
    "assurance.justificatifs.dossier=target/justificatifs-sinistre"
})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SinistreIT {

    private static final String CAMILLE = "recette.auto@assureasy.fr";
    private static final String ALICE = "recette.assure1@assureasy.fr";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final byte[] PDF = "%PDF-1.7\nrecepisse de plainte\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0};

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
    private PurgeBrouillonsSinistre purge;

    @MockitoBean
    private EnvoiSinistre emails;

    private String camille;
    private String alice;

    @BeforeEach
    void preparer() {
        camille = jeton(CAMILLE);
        alice = jeton(ALICE);
    }

    private String jeton(String email) {
        return transaction.execute(etat ->
                "Bearer " + sessionService.ouvrir(comptes.findByEmail(email).orElseThrow()).jeton());
    }

    private ResultActions appel(AbstractMockHttpServletRequestBuilder<?> requete,
            String jeton) throws Exception {
        return mockMvc.perform(requete.header(HttpHeaders.AUTHORIZATION, jeton));
    }

    private long creer(String jeton, String contrat, String type) throws Exception {
        String reponse = appel(post("/sinistres").contentType(JSON)
                .content("{\"numeroContrat\":\"" + contrat + "\",\"type\":\"" + type + "\"}"), jeton)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(reponse, "$.id")).longValue();
    }

    private static String circonstances(String type, LocalDate date, String declarant, String nom, String lien,
            String explication) {
        return """
                {"type":"%s","etape":2,"dateSurvenance":"%s","heureSurvenance":"08:30","lieu":"Parking du travail",
                 "description":"Véhicule disparu le matin","tiersImplique":false,"blesses":false,"declarant":"%s",
                 "declarantNom":%s,"declarantLien":%s,"details":{"Avez-vous déposé plainte ?":"Oui"},
                 "explicationRetard":%s}""".formatted(type, date, declarant, texte(nom), texte(lien),
                texte(explication));
    }

    private static String texte(String valeur) {
        return valeur == null ? "null" : "\"" + valeur + "\"";
    }

    private static LocalDate aujourdhui() {
        return LocalDate.now(ZoneId.of("Europe/Paris"));
    }

    @Test
    @Order(1)
    void donneesDeRecetteCoherentesAvecLesRegles() {
        // Alice : numéro au format RG5, déclaré dans le délai, corbeille habitation, photos manquantes.
        assertThat(jdbc.queryForMap("""
                SELECT s.numero, s.hors_delai, s.corbeille, s.pieces_manquantes, s.tiers_implique, s.blesses
                FROM sinistre s JOIN contrat c ON c.id = s.contrat_id WHERE c.numero = 'AE-RECETTE-1'"""))
                .containsEntry("hors_delai", false).containsEntry("corbeille", "HABITATION")
                .containsEntry("pieces_manquantes", true).containsEntry("tiers_implique", false)
                .containsEntry("blesses", false)
                .hasEntrySatisfying("numero", numero -> assertThat((String) numero).matches("SIN-\\d{4}-\\d{6}"));
        // Camille : brouillon de T-05 à l'étape 2, sans réponse présélectionnée à « qui déclare » ni « blessés ».
        assertThat(jdbc.queryForMap("""
                SELECT etape, declarant, blesses, corbeille FROM sinistre
                WHERE description LIKE '%(brouillon de recette T-05)'"""))
                .hasEntrySatisfying("etape", etape -> assertThat(((Number) etape).intValue()).isEqualTo(2))
                .containsEntry("declarant", null).containsEntry("blesses", null)
                .containsEntry("corbeille", "AUTO");
    }

    @Test
    @Order(2)
    void ac8MesSinistresDeCamilleSansLeBrouillonExpire() throws Exception {
        appel(get("/sinistres"), camille)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].statut").value("BROUILLON"))
                .andExpect(jsonPath("$[0].libelleType").value("Accident sans tiers"))
                .andExpect(jsonPath("$[0].brouillonExpireLe").isNotEmpty());
    }

    @Test
    @Order(3)
    void rg6LaPurgeSupprimeLeBrouillonExpireEtSesPieces() {
        Long expire = jdbc.queryForObject("SELECT id FROM sinistre WHERE description LIKE '%(recette US-24 : purge)'",
                Long.class);
        Long piece = jdbc.queryForObject("""
                INSERT INTO piece_jointe (sinistre_id, type_piece, nom_fichier, format, taille_octets)
                VALUES (?, 'PHOTO_VITRAGE', 'pare-brise.pdf', 'PDF', ?) RETURNING id""", Long.class, expire,
                PDF.length);
        jdbc.update("INSERT INTO piece_jointe_contenu (piece_jointe_id, contenu, taille_octets) VALUES (?, ?, ?)",
                piece, PDF, PDF.length);

        assertThat(purge.purger()).isEqualTo(1);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sinistre WHERE id = ?", Long.class, expire)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM piece_jointe_contenu WHERE piece_jointe_id = ?",
                Long.class, piece)).isZero();
        // Les dossiers déclarés et les brouillons récents restent.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sinistre WHERE statut = 'DECLARE'", Long.class))
                .isPositive();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sinistre WHERE description LIKE '%(brouillon de recette T-05)'",
                Long.class)).isEqualTo(1);
    }

    @Test
    @Order(4)
    void ac1ContratsOuvertsALaDeclarationEtRefusEnAppelDirect() throws Exception {
        appel(get("/sinistres/declaration"), camille)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prenom").value("Camille"))
                .andExpect(jsonPath("$.numeroAssistance").isNotEmpty())
                .andExpect(jsonPath("$.contrats[*].numero").value(hasItem("AE-RECETTE-A04")))
                .andExpect(jsonPath("$.contrats[*].numero").value(hasItem("AE-RECETTE-A09")))
                .andExpect(jsonPath("$.contrats[*].numero").value(hasItem("AE-RECETTE-H01")))
                .andExpect(jsonPath("$.contrats[*].numero").value(not(hasItem("AE-RECETTE-A01"))))
                .andExpect(jsonPath("$.contrats[*].numero").value(not(hasItem("AE-RECETTE-A02"))))
                .andExpect(jsonPath("$.contrats[*].numero").value(not(hasItem("AE-RECETTE-A10"))));

        for (String contrat : List.of("AE-RECETTE-A01", "AE-RECETTE-A02", "AE-RECETTE-A10")) {
            appel(post("/sinistres").contentType(JSON)
                    .content("{\"numeroContrat\":\"" + contrat + "\",\"type\":\"VOL\"}"), camille)
                    .andExpect(status().is(422));
        }
        // Résilié il y a 5 jours : moins de 2 ans, déclarable.
        assertThat(creer(camille, "AE-RECETTE-A09", "VOL")).isPositive();
    }

    @Test
    @Order(5)
    void us04UnClientNeVoitNiNeDeclareSurLesContratsDUnAutre() throws Exception {
        long brouillon = jdbc.queryForObject("SELECT id FROM sinistre WHERE description LIKE '%(brouillon de recette T-05)'",
                Long.class);

        appel(get("/sinistres/{id}", brouillon), alice).andExpect(status().isForbidden());
        appel(put("/sinistres/{id}", brouillon).contentType(JSON).content("{\"type\":\"VOL\"}"), alice)
                .andExpect(status().isForbidden());
        appel(post("/sinistres").contentType(JSON)
                .content("{\"numeroContrat\":\"AE-RECETTE-A03\",\"type\":\"VOL\"}"), alice)
                .andExpect(status().isForbidden());
        appel(get("/sinistres"), alice).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].numeroContrat").value(not(hasItem("AE-RECETTE-A03"))));
    }

    @Test
    @Order(6)
    void ac2TypesDuProduitAutreEnDernierEtPiecesDeRg1() throws Exception {
        appel(get("/contrats/{numero}/types-sinistre", "AE-RECETTE-H01"), camille)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produit").value("HABITATION"))
                .andExpect(jsonPath("$.types.length()").value(7))
                .andExpect(jsonPath("$.types[0].code").value("DEGAT_EAUX"))
                .andExpect(jsonPath("$.types[6].code").value("AUTRE"))
                .andExpect(jsonPath("$.types[0].gestesUrgence[0]").value(containsString("Coupez l'arrivée d'eau")))
                .andExpect(jsonPath("$.types[1].delaiPrevu").value("2 jours ouvrés"));
        appel(get("/contrats/{numero}/types-sinistre", "AE-RECETTE-A04"), camille)
                .andExpect(jsonPath("$.types[0].code").value("ACCIDENT_TIERS"))
                .andExpect(jsonPath("$.types[0].pieces[0].libelle").value("Constat amiable (recto et verso)"))
                .andExpect(jsonPath("$.types[0].pieces[0].obligatoire").value(true))
                .andExpect(jsonPath("$.types[6].code").value("AUTRE"))
                .andExpect(jsonPath("$.types[6].pieces[*].obligatoire").value(not(hasItem(true))));
    }

    @Test
    @Order(7)
    void parcoursCompletVolAvecUneFormuleTiersDeclareParUnProche() throws Exception {
        long id = creer(camille, "AE-RECETTE-A04", "VOL");
        // AC3 : non couvert par la formule Tiers, jamais bloquant.
        appel(get("/sinistres/{id}", id), camille)
                .andExpect(jsonPath("$.controles.couvert").value(false))
                .andExpect(jsonPath("$.statut").value("BROUILLON"))
                .andExpect(jsonPath("$.etape").value(2));

        // AC4 : date dans le futur refusée avec un message clair.
        appel(put("/sinistres/{id}", id).contentType(JSON).content(circonstances("VOL", aujourdhui().plusDays(1),
                "SOUSCRIPTEUR", null, null, null)), camille)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La date du sinistre ne peut pas être dans le futur."));

        // AC13 : déclaré par un proche.
        appel(put("/sinistres/{id}", id).contentType(JSON).content(circonstances("VOL", aujourdhui(), "PROCHE",
                "Léa Durand", "Fille", null)), camille)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controles.horsDelai").value(false))
                .andExpect(jsonPath("$.controles.horsPeriodeContrat").value(false))
                .andExpect(jsonPath("$.declarantNom").value("Léa Durand"))
                .andExpect(jsonPath("$.piecesManquantes").value(true));

        // AC15 : un .exe, ou un .exe renommé en .pdf, est refusé ; AC16 : stockage en erreur, brouillon conservé.
        deposer(id, "facture.exe", EXE).andExpect(status().is(422));
        deposer(id, "facture.pdf", EXE).andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value(containsString("PDF, un JPG ou un PNG")));
        deposer(id, "erreur-stockage.pdf", PDF).andExpect(status().isServiceUnavailable());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM piece_jointe WHERE sinistre_id = ?", Long.class, id))
                .isZero();
        appel(get("/sinistres/{id}", id), camille).andExpect(status().isOk());

        // AC14 : pièce stockée en base avec son empreinte, relue à l'identique.
        String reponse = deposer(id, "recepisse.pdf", PDF).andExpect(status().isOk())
                .andExpect(jsonPath("$.piecesJointes[0].nomFichier").value("recepisse.pdf"))
                .andExpect(jsonPath("$.piecesManquantes").value(false))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long piece = ((Number) JsonPath.read(reponse, "$.piecesJointes[0].id")).longValue();
        assertThat(jdbc.queryForObject("SELECT empreinte FROM piece_jointe WHERE id = ?", String.class, piece))
                .isEqualTo(PiecesJointesSinistreService.empreinte(PDF));
        assertThat(jdbc.queryForObject("SELECT cle_stockage FROM piece_jointe WHERE id = ?", String.class, piece))
                .isEqualTo("base:piece_jointe_contenu/" + piece);
        appel(get("/sinistres/{id}/pieces/{piece}", id, piece), camille)
                .andExpect(status().isOk()).andExpect(content().bytes(PDF));
        appel(get("/sinistres/{id}/pieces/{piece}", id, piece), alice).andExpect(status().isForbidden());

        // AC9, RG8 : sans la déclaration sur l'honneur, rien n'est envoyé.
        appel(post("/sinistres/{id}/envoi", id).contentType(JSON).content("{\"declarationHonneur\":false}"), camille)
                .andExpect(status().isBadRequest());
        verify(emails, never()).accuserReception(any());

        appel(post("/sinistres/{id}/envoi", id).contentType(JSON).content("{\"declarationHonneur\":true}"), camille)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(matchesPattern("SIN-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.statut").value("DECLARE"))
                .andExpect(jsonPath("$.emailEnvoye").value(true));
        assertThat(jdbc.queryForMap("SELECT corbeille, a_requalifier, declaration_honneur, pieces_manquantes, "
                + "declarant FROM sinistre WHERE id = ?", id))
                .containsEntry("corbeille", "AUTO").containsEntry("a_requalifier", false)
                .containsEntry("declaration_honneur", true).containsEntry("pieces_manquantes", false)
                .containsEntry("declarant", "PROCHE");

        // AC9 et AC13 : accusé de réception et information du souscripteur, sans donnée de santé.
        ArgumentCaptor<EnvoiSinistre.Dossier> dossier = ArgumentCaptor.forClass(EnvoiSinistre.Dossier.class);
        verify(emails).accuserReception(dossier.capture());
        verify(emails).informerSouscripteur(any());
        assertThat(dossier.getValue().email()).isEqualTo(CAMILLE);
        assertThat(dossier.getValue().toString().toLowerCase(Locale.ROOT)).doesNotContain("bless", "disparu");

        // Une déclaration envoyée ne se modifie plus.
        appel(put("/sinistres/{id}", id).contentType(JSON).content(circonstances("VOL", aujourdhui(),
                "SOUSCRIPTEUR", null, null, null)), camille).andExpect(status().isConflict());
    }

    @Test
    @Order(8)
    void ac6Ac11Rg11DeclarationTardiveAutreAvecBlesses() throws Exception {
        long id = creer(camille, "AE-RECETTE-A03", "AUTRE");
        String tardive = circonstances("AUTRE", aujourdhui().minusDays(20), "SOUSCRIPTEUR", null, null, null)
                .replace("\"blesses\":false", "\"blesses\":true,\"nombreBlesses\":1");
        appel(put("/sinistres/{id}", id).contentType(JSON).content(tardive), camille)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controles.horsDelai").value(true))
                .andExpect(jsonPath("$.controles.delaiPrevu").value("5 jours ouvrés"))
                .andExpect(jsonPath("$.controles.couvert").value(nullValue()))
                .andExpect(jsonPath("$.nombreBlesses").value(1));

        // AC6 : l'explication du retard est obligatoire à l'envoi ; la déclaration n'est jamais bloquée ensuite.
        appel(post("/sinistres/{id}/envoi", id).contentType(JSON).content("{\"declarationHonneur\":true}"), camille)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("raison du délai")));
        appel(put("/sinistres/{id}", id).contentType(JSON).content(tardive.replace("\"explicationRetard\":null",
                "\"explicationRetard\":\"J'étais à l'étranger\"")), camille).andExpect(status().isOk());
        appel(post("/sinistres/{id}/envoi", id).contentType(JSON).content("{\"declarationHonneur\":true}"), camille)
                .andExpect(status().isOk());

        assertThat(jdbc.queryForMap("SELECT hors_delai, a_requalifier, blesses, nombre_blesses, pieces_manquantes "
                + "FROM sinistre WHERE id = ?", id))
                .containsEntry("hors_delai", true).containsEntry("a_requalifier", true)
                .containsEntry("blesses", true)
                .hasEntrySatisfying("nombre_blesses", nombre -> assertThat(((Number) nombre).intValue()).isEqualTo(1))
                .containsEntry("pieces_manquantes", false);
    }

    @Test
    @Order(9)
    void ac5DateAvantLEffetDuContratSignaleeSansBloquer() throws Exception {
        long id = creer(camille, "AE-RECETTE-A03", "BRIS_GLACE");
        // A03 a pris effet il y a 60 jours (données T-05).
        appel(put("/sinistres/{id}", id).contentType(JSON).content(circonstances("BRIS_GLACE",
                aujourdhui().minusDays(61), "SOUSCRIPTEUR", null, null, "Découvert au retour de vacances")), camille)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controles.horsPeriodeContrat").value(true));
    }

    @Test
    @Order(10)
    void rg7DixMoAuPlusParFichier() throws Exception {
        long id = creer(camille, "AE-RECETTE-A03", "VOL");
        byte[] constat = Arrays.copyOf(PDF, 12 * 1024 * 1024);

        deposer(id, "constat.pdf", constat).andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value(containsString("10 Mo")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM piece_jointe WHERE sinistre_id = ?", Long.class, id))
                .isZero();
    }

    @Test
    @Order(11)
    void ac8LeTableauDeBordMontreLesBrouillonsAReprendre() throws Exception {
        appel(get("/espace-client/tableau-de-bord"), camille)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sinistres[*].statut").value(hasItem("BROUILLON")))
                .andExpect(jsonPath("$.sinistres[*].statut").value(hasItem("DECLARE")));
    }

    private ResultActions deposer(long id, String nom, byte[] contenu) throws Exception {
        return appel(multipart("/sinistres/{id}/pieces", id)
                .file(new MockMultipartFile("fichier", nom, "application/octet-stream", contenu))
                .param("typePiece", "RECEPISSE_PLAINTE"), camille);
    }
}
