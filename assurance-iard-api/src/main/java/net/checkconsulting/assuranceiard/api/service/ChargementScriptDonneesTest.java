package net.checkconsulting.assuranceiard.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exécute au démarrage un script SQL rejouable de données de test (dossier db/donnees-test), lu par
 * {@link ScriptsDonneesTest}. Commun aux corrections de recette (US-22, US-23...) : chaque sous-classe porte son
 * script, son ordre et l'interrupteur {@code DONNEES_TEST_ACTIVES}.
 */
@Slf4j
public abstract class ChargementScriptDonneesTest implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final String script;
    private final String nom;

    protected ChargementScriptDonneesTest(JdbcTemplate jdbc, String script, String nom) {
        this.jdbc = jdbc;
        this.script = script;
        this.nom = nom;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        jdbc.execute(ScriptsDonneesTest.lire(script));
        log.info("Données de recette {} vérifiées", nom);
    }
}
