package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.checkconsulting.assuranceiard.api.config.DonneesTestProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** T-05 : chargement des données de test du sprint 2. */
class DonneesRecetteSprint2Test {

    private final PasswordEncoder encodeur = mock(PasswordEncoder.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final DonneesRecetteSprint2 donnees =
            new DonneesRecetteSprint2(new DonneesTestProperties(true, "Recette2026"), encodeur, jdbc);

    @Test
    void executeLeScriptAvecLEmpreinteDuMotDePasseDeRecette() {
        when(encodeur.encode("Recette2026")).thenReturn("{bcrypt}empreinte");

        donnees.run(null);

        ArgumentCaptor<String> script = ArgumentCaptor.forClass(String.class);
        verify(jdbc).execute(script.capture());
        assertThat(script.getValue())
                .contains("'{bcrypt}empreinte'")
                .doesNotContain(DonneesRecetteSprint2.JETON_MOT_DE_PASSE)
                .contains("recette.auto@assureasy.fr", "recette.prospect@assureasy.fr");
    }

    @Test
    void leScriptNeRecreeRienSIlADejaEteCharge() {
        assertThat(DonneesRecetteSprint2.lireScript())
                .contains("IF EXISTS (SELECT 1 FROM compte WHERE email = 'recette.auto@assureasy.fr')")
                .doesNotContainIgnoringCase("delete ")
                .doesNotContainIgnoringCase("truncate ");
    }

    @Test
    void sExecuteApresLesComptesDeRecetteDUs04() {
        int ordreUs04 = DonneesRecette.class.getAnnotation(Order.class).value();
        int ordreT05 = DonneesRecetteSprint2.class.getAnnotation(Order.class).value();

        assertThat(ordreT05).isGreaterThan(ordreUs04);
    }
}
