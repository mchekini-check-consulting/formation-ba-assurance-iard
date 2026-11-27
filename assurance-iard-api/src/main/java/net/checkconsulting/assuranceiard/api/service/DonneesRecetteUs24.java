package net.checkconsulting.assuranceiard.api.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * US-24 (recette) : sinistres d'Alice et de Camille cohérents avec la déclaration (numéro SIN-AAAA-XXXXXX, délai,
 * aucune présélection, brouillon expiré pour la purge). Exécuté après les données T-05, US-22 et US-23, avec le même
 * interrupteur ({@code DONNEES_TEST_ACTIVES=true} : local, dev, qualif ; jamais en preprod ni en prod). Rejouable.
 */
@Component
@Order(DonneesRecette.ORDRE + 6)
@ConditionalOnProperty(name = "assurance.donnees-test.actif", havingValue = "true")
public class DonneesRecetteUs24 extends ChargementScriptDonneesTest {

    static final String SCRIPT = "db/donnees-test/donnees_recette_us24.sql";

    public DonneesRecetteUs24(JdbcTemplate jdbc) {
        super(jdbc, SCRIPT, "US-24");
    }
}
