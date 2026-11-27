package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
 * T-05, critères « Terminé quand » : migrations V19 à V27 sur une base vide et sur une base du sprint 1 (sans perte
 * de données), contraintes, puis annulation (scripts db/annulation) et réapplication.
 */
class MigrationsT05IT {

    private static final String DERNIERE_VERSION = "27";
    private static final String VERSION_SPRINT_1 = "18";
    /** Scripts db/annulation dans l'ordre inverse des migrations (V22.1 se place entre V22 et V23). */
    private static final List<String> ORDRE_ANNULATION =
            List.of("27", "26", "25", "24", "23", "22_1", "22", "21", "20", "19");
    private static final List<String> NOUVELLES_TABLES = List.of("ref_vehicule", "ref_plaque_test", "vehicule",
            "conducteur", "devis_auto", "devis_option", "contrat_option", "historique_statut_contrat", "document",
            "echeance", "facture", "avenant", "avenant_ligne", "preuve_signature", "declaration_fva",
            "resiliation_hamon", "sinistre", "piece_jointe");

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
    void lesMigrationsPassentSurUneBaseVide() {
        String base = nouvelleBase("vide");

        MigrateResult resultat = flyway(base, DERNIERE_VERSION).migrate();

        assertThat(resultat.success).isTrue();
        assertThat(resultat.targetSchemaVersion).isEqualTo(DERNIERE_VERSION);
        assertThat(tablesPresentes(jdbc(base))).containsAll(NOUVELLES_TABLES);
    }

    @Test
    void lesMigrationsPassentSurUneBaseDuSprint1SansPerteDeDonnees() {
        String base = nouvelleBase("sprint1");
        flyway(base, VERSION_SPRINT_1).migrate();
        JdbcTemplate jdbc = jdbc(base);
        Sprint1 donnees = remplirSprint1(jdbc);

        flyway(base, DERNIERE_VERSION).migrate();

        Map<String, Object> actif = contrat(jdbc, "AE-2026-000001");
        assertThat(actif).containsEntry("produit", "HABITATION").containsEntry("statut", "ACTIF")
                .containsEntry("compte_id", donnees.compte()).containsEntry("formule", "serenite")
                .containsEntry("jour_prelevement", 10).containsEntry("frequence_paiement", "MENSUEL")
                .containsEntry("adresse_logement", "1 rue du Test");
        assertThat(actif.get("franchise").toString()).isEqualTo("300.00");
        assertThat(actif.get("mandat_sepa_id")).isNotNull();
        assertThat(actif.get("devis_id")).isNotNull();

        // Statut des contrats existants inchangé (décision PO) ; formule reprise de la recommandation.
        Map<String, Object> enAttente = contrat(jdbc, "AE-2026-000002");
        assertThat(enAttente).containsEntry("statut", "EN_ATTENTE_DE_SIGNATURE")
                .containsEntry("formule", "essentielle").containsEntry("mandat_sepa_id", null)
                .containsEntry("jour_prelevement", null);
        assertThat(enAttente.get("franchise").toString()).isEqualTo("150.00");

        assertThat(jdbc.queryForList("""
                SELECT c.numero || ' ' || d.version || ' ' || d.statut || ' ' || d.stocke
                FROM document d JOIN contrat c ON c.id = d.contrat_id
                WHERE d.type = 'CONDITIONS_PARTICULIERES' ORDER BY c.numero""", String.class))
                .containsExactly("AE-2026-000001 1 EN_VIGUEUR false", "AE-2026-000002 1 EN_VIGUEUR false");
        assertThat(jdbc.queryForList(
                "SELECT statut_apres || ' ' || auteur FROM historique_statut_contrat ORDER BY contrat_id",
                String.class)).containsExactly("ACTIF SYSTEME", "EN_ATTENTE_DE_SIGNATURE SYSTEME");

        assertThat(jdbc.queryForList(
                "SELECT ville || ' ' || statut_devis || ' ' || formule_conseillee || ' ' || choix_different_conseil"
                        + " FROM devis_enregistre ORDER BY ville", String.class))
                .containsExactly("Lille EN_COURS confort false", "Lyon EXPIRE confort false",
                        "Paris TRANSFORME confort true");

        assertThat(jdbc.queryForList(
                "SELECT rum || ' ' || compte_id || ' ' || COALESCE(iban_masque, '-') || ' ' || statut"
                        + " FROM mandat_sepa ORDER BY rum", String.class))
                .containsExactly("RUM-1 " + donnees.compte() + " •••• 4821 ACTIF",
                        "RUM-2 " + donnees.compte() + " - ACTIF");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM compte", Integer.class)).isEqualTo(1);
    }

    @Test
    void unContratCreeParLeCodeDuSprint1EstCompleteParLaBase() {
        String base = nouvelleBase("compatibilite");
        flyway(base, VERSION_SPRINT_1).migrate();
        JdbcTemplate jdbc = jdbc(base);
        Sprint1 donnees = remplirSprint1(jdbc);
        flyway(base, DERNIERE_VERSION).migrate();

        // Nouvelle souscription telle que l'écrit le code du sprint 1 (MandatService, ContratService) :
        // mandat et contrat sans les colonnes T-05.
        long dossier = dossier(jdbc, donnees.compte(), recommandation(jdbc, "confort"));
        jdbc.update("""
                INSERT INTO mandat_sepa (dossier_id, rum, iban_debut, iban_fin, jour_prelevement, date_creation,
                                         date_signature)
                VALUES (?, 'RUM-3', 'FR76', '0001', 15, NOW(), NOW())""", dossier);
        insererContratSprint1(jdbc, "AE-2026-000003", dossier, "EN_ATTENTE_DE_SIGNATURE", 500);

        assertThat(jdbc.queryForMap("SELECT compte_id, iban_masque, statut FROM mandat_sepa WHERE rum = 'RUM-3'"))
                .containsEntry("compte_id", donnees.compte()).containsEntry("iban_masque", "•••• 0001")
                .containsEntry("statut", "ACTIF");
        Map<String, Object> contrat = contrat(jdbc, "AE-2026-000003");
        assertThat(contrat).containsEntry("produit", "HABITATION").containsEntry("compte_id", donnees.compte())
                .containsEntry("formule", "confort").containsEntry("jour_prelevement", 15)
                .containsEntry("frequence_paiement", "MENSUEL").containsEntry("justificatifs_recus", false);
        assertThat(contrat.get("mandat_sepa_id")).isNotNull();
        assertThat(contrat.get("franchise").toString()).isEqualTo("500.00");
    }

    @Test
    void lesContraintesRefusentLesDonneesIncoherentes() {
        String base = nouvelleBase("contraintes");
        flyway(base, DERNIERE_VERSION).migrate();
        JdbcTemplate jdbc = jdbc(base);
        long compte = jdbc.queryForObject("""
                INSERT INTO compte (email, mot_de_passe_hash, date_creation)
                VALUES ('contraintes@test.fr', 'hash', NOW()) RETURNING id""", Long.class);
        long contrat = jdbc.queryForObject("""
                INSERT INTO contrat (numero, produit, compte_id, statut, formule, adresse_logement, code_postal, ville,
                                     type_logement, surface, franchise, cotisation_mensuelle, cotisation_annuelle,
                                     date_effet)
                VALUES ('AE-2026-000001', 'HABITATION', ?, 'ACTIF', 'confort', '1 rue', '75001', 'Paris',
                        'appartement', 40, 300, 10, 120, CURRENT_DATE) RETURNING id""", Long.class, compte);
        jdbc.update("INSERT INTO document (contrat_id, type) VALUES (?, 'CONDITIONS_PARTICULIERES')", contrat);
        jdbc.update("""
                INSERT INTO facture (numero, contrat_id, periode_debut, periode_fin, montant_ht, taxe, montant_ttc,
                                     generee_le)
                VALUES ('FAC-2026-000001', ?, CURRENT_DATE, CURRENT_DATE, 10, 1, 11, NOW())""", contrat);
        jdbc.update("""
                INSERT INTO avenant (numero, contrat_id, date_effet, prime_avant_ttc, prime_apres_ttc)
                VALUES (1, ?, CURRENT_DATE, 10, 12)""", contrat);
        long sinistre = jdbc.queryForObject("""
                INSERT INTO sinistre (contrat_id, type, date_survenance)
                VALUES (?, 'DEGAT_EAUX', CURRENT_DATE) RETURNING id""", Long.class, contrat);
        long devisAuto = jdbc.queryForObject("""
                INSERT INTO devis_enregistre (compte_id, produit, date_creation, date_fin_validite,
                                              date_dernier_contact)
                VALUES (?, 'AUTO', NOW(), CURRENT_DATE + 30, NOW()) RETURNING id""", Long.class, compte);
        jdbc.update("""
                INSERT INTO mandat_sepa (compte_id, rum, date_creation)
                VALUES (?, 'RUM-AUTO', NOW())""", compte);

        refuse(jdbc, "numéro de contrat en double", """
                INSERT INTO contrat (numero, compte_id, statut, formule, adresse_logement, code_postal, ville,
                                     type_logement, surface, franchise, cotisation_mensuelle, cotisation_annuelle,
                                     date_effet)
                VALUES ('AE-2026-000001', ?, 'ACTIF', 'confort', '1 rue', '75001', 'Paris', 'appartement', 40, 300,
                        10, 120, CURRENT_DATE)""", compte);
        refuse(jdbc, "numéro de facture en double", """
                INSERT INTO facture (numero, contrat_id, periode_debut, periode_fin, montant_ht, taxe, montant_ttc,
                                     generee_le)
                VALUES ('FAC-2026-000001', ?, CURRENT_DATE, CURRENT_DATE, 10, 1, 11, NOW())""", contrat);
        refuse(jdbc, "2e version EN_VIGUEUR",
                "INSERT INTO document (contrat_id, type, version) VALUES (?, 'CONDITIONS_PARTICULIERES', 2)", contrat);
        refuse(jdbc, "2e avenant EN_ATTENTE", """
                INSERT INTO avenant (numero, contrat_id, date_effet, prime_avant_ttc, prime_apres_ttc)
                VALUES (2, ?, CURRENT_DATE, 10, 12)""", contrat);
        refuse(jdbc, "TTC différent de HT + taxe", """
                INSERT INTO echeance (contrat_id, date_echeance, montant_ht, taxe, montant_ttc)
                VALUES (?, CURRENT_DATE, 10, 1, 12)""", contrat);
        refuse(jdbc, "statut de contrat inconnu", "UPDATE contrat SET statut = 'SIGNE' WHERE id = ?", contrat);
        refuse(jdbc, "jour de prélèvement hors 5, 10, 15",
                "UPDATE contrat SET jour_prelevement = 7 WHERE id = ?", contrat);
        refuse(jdbc, "contrat signé sans mandat", "UPDATE contrat SET signe_le = NOW() WHERE id = ?", contrat);
        refuse(jdbc, "contrat AUTO sans véhicule ni conducteur",
                "UPDATE contrat SET produit = 'AUTO' WHERE id = ?", contrat);
        refuse(jdbc, "contrat HABITATION sans logement",
                "UPDATE contrat SET ville = NULL WHERE id = ?", contrat);
        refuse(jdbc, "bonus-malus hors 0,50 - 3,50",
                "INSERT INTO conducteur (coefficient_bonus_malus) VALUES (?)", new java.math.BigDecimal("3.60"));
        refuse(jdbc, "acquisition avant la mise en circulation", """
                INSERT INTO vehicule (marque, modele, energie, source, date_mise_circulation, date_acquisition)
                VALUES ('Renault', 'Clio', 'ESSENCE', 'PLAQUE', DATE '2024-01-01', DATE '2023-12-31')""");
        refuse(jdbc, "acquisition dans le futur", """
                INSERT INTO vehicule (marque, modele, energie, source, date_acquisition)
                VALUES ('Renault', 'Clio', 'ESSENCE', 'PLAQUE', CURRENT_DATE + 1)""");
        refuse(jdbc, "immatriculation mal formée", """
                INSERT INTO vehicule (marque, modele, energie, source, immatriculation)
                VALUES ('Renault', 'Clio', 'ESSENCE', 'PLAQUE', 'AB123CD')""");
        refuse(jdbc, "sinistre dans le futur", """
                INSERT INTO sinistre (contrat_id, type, date_survenance) VALUES (?, 'DEGAT_EAUX', CURRENT_DATE + 1)""",
                contrat);
        refuse(jdbc, "sinistre DECLARE sans déclaration sur l'honneur", """
                INSERT INTO sinistre (numero, contrat_id, statut, type, date_survenance, declare_le)
                VALUES ('SIN-2026-000001', ?, 'DECLARE', 'DEGAT_EAUX', CURRENT_DATE, NOW())""", contrat);
        // US-24 (V36) : 10 Mo au plus par pièce jointe (RG7, décision BA du 26/11/2026), au lieu de 5 Mo.
        refuse(jdbc, "pièce jointe de plus de 10 Mo", """
                INSERT INTO piece_jointe (sinistre_id, type_piece, nom_fichier, format, taille_octets)
                VALUES (?, 'PHOTO', 'photo.jpg', 'JPG', 10485761)""", sinistre);
        refuse(jdbc, "pièce jointe dans un format refusé", """
                INSERT INTO piece_jointe (sinistre_id, type_piece, nom_fichier, format, taille_octets)
                VALUES (?, 'PHOTO', 'photo.gif', 'GIF', 1000)""", sinistre);
        refuse(jdbc, "devis auto : ancien assureur manquant",
                "INSERT INTO devis_auto (devis_id, assure_plus_un_an) VALUES (?, TRUE)", devisAuto);
        refuse(jdbc, "devis auto : date d'effet au-delà de 90 jours",
                "INSERT INTO devis_auto (devis_id, date_effet_souhaitee) VALUES (?, CURRENT_DATE + 91)", devisAuto);
        refuse(jdbc, "devis habitation sans logement", """
                INSERT INTO devis_enregistre (compte_id, date_creation, date_fin_validite, date_dernier_contact)
                VALUES (?, NOW(), CURRENT_DATE + 30, NOW())""", compte);
        refuse(jdbc, "statut de mandat inconnu", "UPDATE mandat_sepa SET statut = 'SUSPENDU' WHERE rum = 'RUM-AUTO'");
        refuse(jdbc, "numéro de RUM en double",
                "INSERT INTO mandat_sepa (compte_id, rum, date_creation) VALUES (?, 'RUM-AUTO', NOW())", compte);
        refuse(jdbc, "pas de suppression en cascade d'un contrat (documents, sinistres...)",
                "DELETE FROM contrat WHERE id = ?", contrat);

        // Les règles acceptent bien les cas valides.
        jdbc.update("UPDATE mandat_sepa SET statut = 'REVOQUE' WHERE rum = 'RUM-AUTO'");
        jdbc.update("INSERT INTO devis_auto (devis_id, date_effet_souhaitee) VALUES (?, CURRENT_DATE + 90)", devisAuto);
        jdbc.update("UPDATE document SET statut = 'REMPLACE' WHERE contrat_id = ?", contrat);
        jdbc.update("INSERT INTO document (contrat_id, type, version) VALUES (?, 'CONDITIONS_PARTICULIERES', 2)",
                contrat);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM document WHERE contrat_id = ?", Integer.class, contrat))
                .isEqualTo(2);
    }

    @Test
    void chaqueMigrationPeutEtreAnnuleePuisRejouee() throws IOException {
        String base = nouvelleBase("annulation");
        flyway(base, VERSION_SPRINT_1).migrate();
        JdbcTemplate jdbc = jdbc(base);
        remplirSprint1(jdbc);
        List<Map<String, Object>> contratsAvant = jdbc.queryForList(
                "SELECT numero, statut, franchise, ville FROM contrat ORDER BY numero");
        flyway(base, DERNIERE_VERSION).migrate();

        for (String version : ORDRE_ANNULATION) {
            executerAnnulation(jdbc, version);
        }

        assertThat(jdbc.queryForObject("SELECT MAX(version::NUMERIC) FROM flyway_schema_history", Integer.class))
                .isEqualTo(18);
        assertThat(tablesPresentes(jdbc)).doesNotContainAnyElementsOf(NOUVELLES_TABLES);
        assertThat(jdbc.queryForObject("""
                SELECT data_type FROM information_schema.columns
                WHERE table_name = 'contrat' AND column_name = 'franchise'""", String.class)).isEqualTo("integer");
        assertThat(jdbc.queryForList("SELECT numero, statut, franchise, ville FROM contrat ORDER BY numero"))
                .isEqualTo(contratsAvant);

        MigrateResult rejeu = flyway(base, DERNIERE_VERSION).migrate();
        assertThat(rejeu.success).isTrue();
        assertThat(rejeu.migrationsExecuted).isEqualTo(ORDRE_ANNULATION.size());
    }

    @Test
    void lAnnulationDuContratSArreteSIlExisteDesContratsAuto() throws IOException {
        String base = nouvelleBase("annulation_refusee");
        flyway(base, DERNIERE_VERSION).migrate();
        JdbcTemplate jdbc = jdbc(base);
        long compte = jdbc.queryForObject("""
                INSERT INTO compte (email, mot_de_passe_hash, date_creation)
                VALUES ('auto@test.fr', 'hash', NOW()) RETURNING id""", Long.class);
        long vehicule = jdbc.queryForObject(
                "INSERT INTO vehicule (marque, modele, energie, source) VALUES ('Peugeot', '208', 'GPL', 'MARQUE')"
                        + " RETURNING id", Long.class);
        long conducteur = jdbc.queryForObject("INSERT INTO conducteur DEFAULT VALUES RETURNING id", Long.class);
        jdbc.update("""
                INSERT INTO contrat (numero, produit, compte_id, statut, formule, franchise, cotisation_mensuelle,
                                     cotisation_annuelle, date_effet, vehicule_id, conducteur_principal_id)
                VALUES ('AE-2026-000009', 'AUTO', ?, 'ACTIF', 'TIERS', 0, 30, 360, CURRENT_DATE, ?, ?)""",
                compte, vehicule, conducteur);
        for (String version : ORDRE_ANNULATION.subList(0, ORDRE_ANNULATION.indexOf("23"))) {
            executerAnnulation(jdbc, version);
        }

        assertThatThrownBy(() -> executerAnnulation(jdbc, "23")).hasMessageContaining("Annulation V23 impossible");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contrat WHERE produit = 'AUTO'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void unContratSigneEstRemisEnConformiteSelonSaDateDEffet() {
        String base = nouvelleBase("statut_signe");
        flyway(base, "22").migrate();
        JdbcTemplate jdbc = jdbc(base);
        long compte = remplirSprint1(jdbc).compte();
        insererContratSigne(jdbc, compte, "AE-2026-000010", "CURRENT_DATE - 30");
        insererContratSigne(jdbc, compte, "AE-2026-000011", "CURRENT_DATE");
        insererContratSigne(jdbc, compte, "AE-2026-000012", "CURRENT_DATE + 15");

        MigrateResult resultat = flyway(base, DERNIERE_VERSION).migrate();

        assertThat(resultat.success).isTrue();
        assertThat(contrat(jdbc, "AE-2026-000010")).containsEntry("statut", "ACTIF");
        assertThat(contrat(jdbc, "AE-2026-000011")).containsEntry("statut", "ACTIF");
        assertThat(contrat(jdbc, "AE-2026-000012")).containsEntry("statut", "EN_ATTENTE_EFFET");
        // Les statuts valides ne sont pas touchés.
        assertThat(contrat(jdbc, "AE-2026-000002")).containsEntry("statut", "EN_ATTENTE_DE_SIGNATURE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contrat WHERE statut = 'SIGNE'", Integer.class)).isZero();
    }

    @Test
    void unStatutInconnuArreteLaMigrationEnListantLesValeurs() {
        String base = nouvelleBase("statut_inconnu");
        flyway(base, "22").migrate();
        JdbcTemplate jdbc = jdbc(base);
        long compte = remplirSprint1(jdbc).compte();
        insererContratSigne(jdbc, compte, "AE-2026-000020", "CURRENT_DATE");
        jdbc.update("UPDATE contrat SET statut = 'BROUILLON' WHERE numero = 'AE-2026-000001'");
        jdbc.update("UPDATE contrat SET statut = 'ANNULE' WHERE numero = 'AE-2026-000002'");

        assertThatThrownBy(() -> flyway(base, DERNIERE_VERSION).migrate())
                .hasStackTraceContaining("V22.1 : statuts de contrat inconnus à traiter avant V23 : ANNULE, BROUILLON");
        // La migration est annulée en bloc : la base reste en V22, le contrat SIGNE n'est pas modifié.
        assertThat(jdbc.queryForObject(
                "SELECT MAX(version::NUMERIC) FROM flyway_schema_history WHERE success", Integer.class)).isEqualTo(22);
        assertThat(contrat(jdbc, "AE-2026-000020")).containsEntry("statut", "SIGNE");
    }

    private static void insererContratSigne(JdbcTemplate jdbc, long compte, String numero, String dateEffet) {
        long dossier = dossier(jdbc, compte, recommandation(jdbc, "confort"));
        jdbc.update("""
                INSERT INTO contrat (numero, dossier_id, statut, adresse_logement, code_postal, ville, type_logement,
                                     surface, franchise, cotisation_mensuelle, cotisation_annuelle, date_effet)
                VALUES (?, ?, 'SIGNE', '1 rue du Test', '75001', 'Paris', 'appartement', 40, 300, 18.50, 222.00,
                        """ + dateEffet + ")", numero, dossier);
    }

    private record Sprint1(long compte) {
    }

    /** Données telles que les écrit le sprint 1 : 2 contrats (signé, en attente), 3 devis, 2 mandats. */
    private static Sprint1 remplirSprint1(JdbcTemplate jdbc) {
        long compte = jdbc.queryForObject("""
                INSERT INTO compte (email, mot_de_passe_hash, date_creation)
                VALUES ('sprint1@test.fr', 'hash', NOW()) RETURNING id""", Long.class);
        UUID signe = recommandation(jdbc, "confort");
        UUID enAttente = recommandation(jdbc, "essentielle");
        UUID expire = recommandation(jdbc, "confort");
        UUID enCours = recommandation(jdbc, "confort");

        long dossierSigne = dossier(jdbc, compte, signe);
        long dossierEnAttente = dossier(jdbc, compte, enAttente);
        jdbc.update("""
                INSERT INTO mandat_sepa (dossier_id, rum, titulaire, iban_debut, iban_fin, jour_prelevement, date_debut,
                                         date_creation, date_signature)
                VALUES (?, 'RUM-1', 'Jean Test', 'FR76', '4821', 10, CURRENT_DATE, NOW(), NOW())""", dossierSigne);
        jdbc.update("INSERT INTO mandat_sepa (dossier_id, rum, jour_prelevement, date_creation) VALUES (?, 'RUM-2', 7, NOW())",
                dossierEnAttente);
        jdbc.update("""
                INSERT INTO tarif_devis (reference_devis, id_tarif, code_postal, type_logement, surface, franchise,
                                         date_creation, formule, options)
                VALUES (?, ?, '75001', 'appartement', 40, 300, NOW(), 'serenite', '{}')""", signe, UUID.randomUUID());
        devis(jdbc, compte, signe, "serenite", "Paris", 30);
        devis(jdbc, compte, expire, "confort", "Lyon", -1);
        devis(jdbc, compte, enCours, "confort", "Lille", 10);
        insererContratSprint1(jdbc, "AE-2026-000001", dossierSigne, "ACTIF", 300);
        insererContratSprint1(jdbc, "AE-2026-000002", dossierEnAttente, "EN_ATTENTE_DE_SIGNATURE", 150);
        return new Sprint1(compte);
    }

    private static void insererContratSprint1(JdbcTemplate jdbc, String numero, long dossier, String statut,
            int franchise) {
        jdbc.update("""
                INSERT INTO contrat (numero, dossier_id, statut, adresse_logement, code_postal, ville, type_logement,
                                     surface, franchise, cotisation_mensuelle, cotisation_annuelle, date_effet)
                VALUES (?, ?, ?, '1 rue du Test', '75001', 'Paris', 'appartement', 40, ?, 18.50, 222.00,
                        CURRENT_DATE)""", numero, dossier, statut, franchise);
    }

    private static UUID recommandation(JdbcTemplate jdbc, String formule) {
        UUID reference = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO recommandation_devis (reference, valeur_contenu, objets_valeur, appareils_couteux,
                                                  remboursement, assistance, litiges, priorite, formule, raisons,
                                                  options, date_creation)
                VALUES (?, 20000, FALSE, FALSE, 'neuf', FALSE, FALSE, 'prix', ?, '{}', '{}', NOW())""",
                reference, formule);
        return reference;
    }

    private static long dossier(JdbcTemplate jdbc, long compte, UUID reference) {
        return jdbc.queryForObject("""
                INSERT INTO dossier_souscription (compte_id, reference_devis, colocataires, date_creation)
                VALUES (?, ?, '{}', NOW()) RETURNING id""", Long.class, compte, reference);
    }

    private static void devis(JdbcTemplate jdbc, long compte, UUID reference, String formule, String ville,
            int joursDeValidite) {
        jdbc.update("""
                INSERT INTO devis_enregistre (compte_id, reference, type_logement, adresse, code_postal, ville, surface,
                                              formule, options, franchise, prix_mensuel, date_creation,
                                              date_fin_validite, date_dernier_contact)
                VALUES (?, ?, 'appartement', '1 rue', '75001', ?, 40, ?, '{}', '300', 18.50, NOW(),
                        CURRENT_DATE + ?, NOW())""", compte, reference, ville, formule, joursDeValidite);
    }

    private static Map<String, Object> contrat(JdbcTemplate jdbc, String numero) {
        return jdbc.queryForMap("SELECT * FROM contrat WHERE numero = ?", numero);
    }

    private static void refuse(JdbcTemplate jdbc, String regle, String sql, Object... parametres) {
        assertThatThrownBy(() -> jdbc.update(sql, parametres)).as(regle)
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static void executerAnnulation(JdbcTemplate jdbc, String version) throws IOException {
        try (var fichiers = Files.list(Path.of("db", "annulation"))) {
            Path script = fichiers.filter(f -> f.getFileName().toString().startsWith("U" + version + "__"))
                    .findFirst().orElseThrow();
            jdbc.execute(Files.readString(script, StandardCharsets.UTF_8));
        }
    }

    private static List<String> tablesPresentes(JdbcTemplate jdbc) {
        return jdbc.queryForList("SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class);
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
