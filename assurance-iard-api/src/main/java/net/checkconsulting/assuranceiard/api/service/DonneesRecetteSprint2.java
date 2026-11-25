package net.checkconsulting.assuranceiard.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.config.DonneesTestProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * T-05 : données de test du sprint 2 (contrats auto dans chaque statut, devis auto, avenant, échéances, facture,
 * sinistres...), en complément des comptes de recette d'US-04. Même interrupteur que {@link DonneesRecette}
 * (DONNEES_TEST_ACTIVES : local, dev, qualif ; jamais en preprod ni en prod) et exécuté juste après lui.
 * Le script ne recrée rien s'il a déjà été chargé.
 */
@Slf4j
@Component
@Order(DonneesRecette.ORDRE + 1)
@ConditionalOnProperty(name = "assurance.donnees-test.actif", havingValue = "true")
@RequiredArgsConstructor
public class DonneesRecetteSprint2 implements ApplicationRunner {

    static final String SCRIPT = "db/donnees-test/donnees_recette_t05.sql";
    static final String JETON_MOT_DE_PASSE = "__MOT_DE_PASSE_HASH__";

    private final DonneesTestProperties reglages;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        String script = lireScript().replace(JETON_MOT_DE_PASSE, passwordEncoder.encode(reglages.motDePasse()));
        jdbc.execute(script);
        log.info("Données de recette T-05 vérifiées");
    }

    static String lireScript() {
        return ScriptsDonneesTest.lire(SCRIPT);
    }
}
