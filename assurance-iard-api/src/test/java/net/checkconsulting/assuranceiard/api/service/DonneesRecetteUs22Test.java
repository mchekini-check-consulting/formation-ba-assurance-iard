package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class DonneesRecetteUs22Test {

    @Test
    void executeLeScriptDeDonneesDeTestDAlice() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);

        new DonneesRecetteUs22(jdbc).run(null);

        verify(jdbc).execute(ScriptsDonneesTest.lire(DonneesRecetteUs22.SCRIPT));
    }

    @Test
    void leScriptNeTouchQueLeContratDAliceEtNeSupprimeRien() {
        String script = ScriptsDonneesTest.lire(DonneesRecetteUs22.SCRIPT);

        assertThat(script).contains("AE-RECETTE-1", "FAC-RECETTE-0001", "UPDATE contrat SET date_effet",
                "statut = 'PAYEE'");
        assertThat(script).doesNotContainIgnoringCase("DELETE").doesNotContainIgnoringCase("DROP");
    }

    @Test
    void unScriptIntrouvableEstSignaleClairement() {
        assertThatThrownBy(() -> ScriptsDonneesTest.lire("db/donnees-test/absent.sql"))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("db/donnees-test/absent.sql");
    }
}
