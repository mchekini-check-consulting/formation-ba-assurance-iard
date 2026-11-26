package net.checkconsulting.assuranceiard.api.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * US-23 (recette) : contrats de Camille cohérents avec l'avenant (H01 : avenant en attente sur une option habitation
 * de la grille ; A03 : une option à retirer). Exécuté après les données T-05 et US-22, avec le même interrupteur
 * ({@code DONNEES_TEST_ACTIVES=true} : local, dev, qualif ; jamais en preprod ni en prod). Le script est rejouable.
 */
@Component
@Order(DonneesRecette.ORDRE + 5)
@ConditionalOnProperty(name = "assurance.donnees-test.actif", havingValue = "true")
public class DonneesRecetteUs23 extends ChargementScriptDonneesTest {

    static final String SCRIPT = "db/donnees-test/donnees_recette_us23.sql";

    public DonneesRecetteUs23(JdbcTemplate jdbc) {
        super(jdbc, SCRIPT, "US-23");
    }
}
