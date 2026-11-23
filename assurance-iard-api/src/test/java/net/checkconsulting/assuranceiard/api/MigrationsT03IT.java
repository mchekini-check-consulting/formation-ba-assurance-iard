package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * T-03 : migration V28 (champ reference, nouvelle règle d'unicité, table document_contenu) sur une base T-05 sans
 * perte, contraintes, puis annulation (db/annulation/U28) et réapplication.
 */
class MigrationsT03IT {

    private static final String V27 = "27";
    private static final String V28 = "28";

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16"));

    @BeforeAll
    static void demarrer() {
        POSTGRES.start();
    }

    @AfterAll
    static void arreter() {
        POSTGRES.stop();
    }

    @Test
    void v28PasseSurUneBaseT05SansModifierLesDocuments() {
        String base = nouvelleBase("t05");
        flyway(base, V27).migrate();
        JdbcTemplate jdbc = jdbc(base);
        long contrat = contrat(jdbc, "AE-2026-000001");
        jdbc.update("INSERT INTO document (contrat_id, type, version, statut) VALUES (?, 'CONDITIONS_PARTICULIERES', 1,"
                + " 'REMPLACE')", contrat);
        jdbc.update("INSERT INTO document (contrat_id, type, version) VALUES (?, 'CONDITIONS_PARTICULIERES', 2)",
                contrat);
        List<String> avant = documents(jdbc);

        MigrateResult resultat = flyway(base, V28).migrate();

        assertThat(resultat.success).isTrue();
        assertThat(documents(jdbc)).isEqualTo(avant);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM document WHERE reference IS NOT NULL", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM document_contenu", Integer.class)).isZero();
    }

    @Test
    void laNouvelleRegleDUniciteAcceptePlusieursFacturesEtUneSeuleVersionEnVigueur() {
        String base = nouvelleBase("unicite");
        flyway(base, V28).migrate();
        JdbcTemplate jdbc = jdbc(base);
        long contrat = contrat(jdbc, "AE-2026-000001");

        jdbc.update("INSERT INTO document (contrat_id, type, reference) VALUES (?, 'FACTURE', 'FAC-2026-000001')",
                contrat);
        jdbc.update("INSERT INTO document (contrat_id, type, reference) VALUES (?, 'FACTURE', 'FAC-2026-000002')",
                contrat);
        jdbc.update("INSERT INTO document (contrat_id, type, reference) VALUES (?, 'AVENANT', '1')", contrat);
        jdbc.update("INSERT INTO document (contrat_id, type, reference) VALUES (?, 'AVENANT', '2')", contrat);
        jdbc.update("INSERT INTO document (contrat_id, type) VALUES (?, 'CONDITIONS_PARTICULIERES')", contrat);
        jdbc.update("INSERT INTO document (type, reference) VALUES ('CONDITIONS_GENERALES', 'HABITATION')");
        jdbc.update("INSERT INTO document (type, reference) VALUES ('CONDITIONS_GENERALES', 'AUTO')");
        // Documents sans contrat ni référence (sinistre, devis) : libres, comme avant V28.
        jdbc.update("INSERT INTO document (type) VALUES ('DECLARATION_SINISTRE')");
        jdbc.update("INSERT INTO document (type) VALUES ('DECLARATION_SINISTRE')");

        refuse(jdbc, "même facture en vigueur deux fois",
                "INSERT INTO document (contrat_id, type, reference, version) VALUES (?, 'FACTURE', 'FAC-2026-000001',"
                        + " 2)", contrat);
        refuse(jdbc, "2e version en vigueur des conditions particulières",
                "INSERT INTO document (contrat_id, type, version) VALUES (?, 'CONDITIONS_PARTICULIERES', 2)", contrat);
        refuse(jdbc, "2e version en vigueur des conditions générales d'un produit",
                "INSERT INTO document (type, reference, version) VALUES ('CONDITIONS_GENERALES', 'AUTO', 2)");

        // Remplacer puis créer la version suivante reste possible.
        jdbc.update("UPDATE document SET statut = 'REMPLACE' WHERE type = 'CONDITIONS_GENERALES' AND reference = 'AUTO'");
        jdbc.update("INSERT INTO document (type, reference, version) VALUES ('CONDITIONS_GENERALES', 'AUTO', 2)");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM document WHERE reference = 'AUTO'", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void leContenuDUnPdfEstControle() {
        String base = nouvelleBase("contenu");
        flyway(base, V28).migrate();
        JdbcTemplate jdbc = jdbc(base);
        long document = jdbc.queryForObject(
                "INSERT INTO document (type, reference) VALUES ('CONDITIONS_GENERALES', 'AUTO') RETURNING id",
                Long.class);

        jdbc.update("INSERT INTO document_contenu (document_id, contenu, taille_octets) VALUES (?, '\\x255044', 3)",
                document);
        jdbc.update("UPDATE document SET stocke = TRUE, cle_stockage = 'base:document_contenu/' || id,"
                + " empreinte = 'e' WHERE id = ?", document);

        refuse(jdbc, "deux contenus pour le même document",
                "INSERT INTO document_contenu (document_id, contenu, taille_octets) VALUES (?, '\\x01', 1)", document);
        refuse(jdbc, "contenu d'un document inconnu",
                "INSERT INTO document_contenu (document_id, contenu, taille_octets) VALUES (999, '\\x01', 1)");
        long autre = jdbc.queryForObject(
                "INSERT INTO document (type, reference) VALUES ('CONDITIONS_GENERALES', 'HABITATION') RETURNING id",
                Long.class);
        refuse(jdbc, "PDF vide",
                "INSERT INTO document_contenu (document_id, contenu, taille_octets) VALUES (?, '\\x', 0)", autre);
        refuse(jdbc, "taille annoncée différente de la taille réelle",
                "INSERT INTO document_contenu (document_id, contenu, taille_octets) VALUES (?, '\\x0102', 1)", autre);
        refuse(jdbc, "PDF de plus de 5 Mo", """
                INSERT INTO document_contenu (document_id, contenu, taille_octets)
                VALUES (?, DECODE(REPEAT('00', 5242881), 'hex'), 5242881)""", autre);
        jdbc.update("""
                INSERT INTO document_contenu (document_id, contenu, taille_octets)
                VALUES (?, DECODE(REPEAT('00', 5242880), 'hex'), 5242880)""", autre);
    }

    @Test
    void v28PeutEtreAnnuleePuisRejouee() throws IOException {
        String base = nouvelleBase("annulation");
        flyway(base, V28).migrate();
        JdbcTemplate jdbc = jdbc(base);
        long contrat = contrat(jdbc, "AE-2026-000001");
        long cp = jdbc.queryForObject("""
                INSERT INTO document (contrat_id, type, stocke, cle_stockage, empreinte)
                VALUES (?, 'CONDITIONS_PARTICULIERES', TRUE, 'base:document_contenu/0', 'e') RETURNING id""",
                Long.class, contrat);
        jdbc.update("INSERT INTO document_contenu (document_id, contenu, taille_octets) VALUES (?, '\\x01', 1)", cp);
        jdbc.update("INSERT INTO document (type, reference) VALUES ('CONDITIONS_GENERALES', 'AUTO')");

        annuler(jdbc);

        assertThat(jdbc.queryForObject("SELECT MAX(version::NUMERIC) FROM flyway_schema_history", Integer.class))
                .isEqualTo(27);
        assertThat(jdbc.queryForList("SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class))
                .doesNotContain("document_contenu");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = 'document' AND column_name = 'reference'""", Integer.class)).isZero();
        // Le document reste, à regénérer ; les conditions générales provisoires sont retirées.
        assertThat(documents(jdbc)).containsExactly("CONDITIONS_PARTICULIERES 1 EN_VIGUEUR false");
        refuse(jdbc, "ancienne règle d'unicité rétablie",
                "INSERT INTO document (contrat_id, type, version) VALUES (?, 'CONDITIONS_PARTICULIERES', 2)", contrat);

        MigrateResult rejeu = flyway(base, V28).migrate();
        assertThat(rejeu.success).isTrue();
        assertThat(rejeu.migrationsExecuted).isEqualTo(1);
    }

    @Test
    void lAnnulationSArreteSiUnContratAPlusieursFacturesEnVigueur() {
        String base = nouvelleBase("annulation_refusee");
        flyway(base, V28).migrate();
        JdbcTemplate jdbc = jdbc(base);
        long contrat = contrat(jdbc, "AE-2026-000001");
        jdbc.update("INSERT INTO document (contrat_id, type, reference) VALUES (?, 'FACTURE', 'FAC-1')", contrat);
        jdbc.update("INSERT INTO document (contrat_id, type, reference) VALUES (?, 'FACTURE', 'FAC-2')", contrat);

        assertThatThrownBy(() -> annuler(jdbc)).hasMessageContaining("Annulation V28 impossible");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM document WHERE reference LIKE 'FAC-%'", Integer.class))
                .isEqualTo(2);
    }

    private static List<String> documents(JdbcTemplate jdbc) {
        return jdbc.queryForList("SELECT type || ' ' || version || ' ' || statut || ' ' || stocke FROM document"
                + " ORDER BY id", String.class);
    }

    private static long contrat(JdbcTemplate jdbc, String numero) {
        long compte = jdbc.queryForObject("""
                INSERT INTO compte (email, mot_de_passe_hash, date_creation)
                VALUES (?, 'hash', NOW()) RETURNING id""", Long.class, numero + "@test.fr");
        return jdbc.queryForObject("""
                INSERT INTO contrat (numero, produit, compte_id, statut, formule, adresse_logement, code_postal, ville,
                                     type_logement, surface, franchise, cotisation_mensuelle, cotisation_annuelle,
                                     date_effet)
                VALUES (?, 'HABITATION', ?, 'ACTIF', 'confort', '1 rue', '75001', 'Paris', 'appartement', 40, 300, 10,
                        120, CURRENT_DATE) RETURNING id""", Long.class, numero, compte);
    }

    private static void refuse(JdbcTemplate jdbc, String regle, String sql, Object... parametres) {
        assertThatThrownBy(() -> jdbc.update(sql, parametres)).as(regle)
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static void annuler(JdbcTemplate jdbc) throws IOException {
        jdbc.execute(Files.readString(Path.of("db", "annulation", "U28__documents_generes.sql"),
                StandardCharsets.UTF_8));
    }

    private static String nouvelleBase(String nom) {
        jdbc(POSTGRES.getDatabaseName()).execute("CREATE DATABASE " + nom);
        return nom;
    }

    private static JdbcTemplate jdbc(String base) {
        return new JdbcTemplate(new DriverManagerDataSource(url(base), POSTGRES.getUsername(),
                POSTGRES.getPassword()));
    }

    private static Flyway flyway(String base, String version) {
        return Flyway.configure()
                .dataSource(url(base), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target(version)
                .load();
    }

    private static String url(String base) {
        return "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/" + base;
    }
}
