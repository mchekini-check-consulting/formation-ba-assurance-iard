package net.checkconsulting.assuranceiard.api.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * US-22 (recette) : données de test d'Alice cohérentes avec sa date d'effet (effet un an plus tôt, échéance du mois en
 * cours payée : une année d'assurance entièrement réglée, facture annuelle testable). Exécuté après les données T-05,
 * avec le même interrupteur ({@code DONNEES_TEST_ACTIVES=true} : local, dev, qualif ; jamais en preprod ni en prod).
 * Le script est rejouable.
 */
@Component
@Order(DonneesRecette.ORDRE + 4)
@ConditionalOnProperty(name = "assurance.donnees-test.actif", havingValue = "true")
public class DonneesRecetteUs22 extends ChargementScriptDonneesTest {

    static final String SCRIPT = "db/donnees-test/donnees_recette_us22.sql";

    public DonneesRecetteUs22(JdbcTemplate jdbc) {
        super(jdbc, SCRIPT, "US-22");
    }
}
