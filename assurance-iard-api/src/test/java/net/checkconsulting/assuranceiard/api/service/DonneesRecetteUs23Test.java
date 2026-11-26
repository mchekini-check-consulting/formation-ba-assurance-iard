package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** US-23 (recette) : le script de données de test de Camille est exécuté, rejouable et ne supprime rien. */
class DonneesRecetteUs23Test {

    @Test
    void executeLeScriptDeRecette() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);

        new DonneesRecetteUs23(jdbc).run(null);

        verify(jdbc).execute(ScriptsDonneesTest.lire(DonneesRecetteUs23.SCRIPT));
    }

    @Test
    void leScriptCorrigeH01EtA03SansRienSupprimer() {
        String script = ScriptsDonneesTest.lire(DonneesRecetteUs23.SCRIPT);

        assertThat(script).contains("AE-RECETTE-H01", "objets-valeur", "AE-RECETTE-A03", "PROTECTION_JURIDIQUE",
                "NOT EXISTS");
        assertThat(script.toUpperCase(java.util.Locale.ROOT)).doesNotContain("DELETE", "TRUNCATE", "DROP ");
    }
}
