package net.checkconsulting.assuranceiard.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-22 (recette) : données de test d'Alice cohérentes avec sa date d'effet (effet un an plus tôt, échéance du mois en
 * cours payée : une année d'assurance entièrement réglée, facture annuelle testable). Exécuté après les données T-05,
 * avec le même interrupteur ({@code DONNEES_TEST_ACTIVES=true} : local, dev, qualif ; jamais en preprod ni en prod).
 * Le script est rejouable.
 */
@Slf4j
@Component
@Order(DonneesRecette.ORDRE + 4)
@ConditionalOnProperty(name = "assurance.donnees-test.actif", havingValue = "true")
@RequiredArgsConstructor
public class DonneesRecetteUs22 implements ApplicationRunner {

    static final String SCRIPT = "db/donnees-test/donnees_recette_us22.sql";

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        jdbc.execute(ScriptsDonneesTest.lire(SCRIPT));
        log.info("Données de recette US-22 vérifiées");
    }
}
