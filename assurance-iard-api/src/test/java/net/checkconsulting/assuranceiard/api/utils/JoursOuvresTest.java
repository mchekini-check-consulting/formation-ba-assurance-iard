package net.checkconsulting.assuranceiard.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** US-24, RG2 : jours ouvrés (hors samedis, dimanches et jours fériés nationaux). */
class JoursOuvresTest {

    @ParameterizedTest
    @CsvSource({"2024, 2024-03-31", "2025, 2025-04-20", "2026, 2026-04-05", "2027, 2027-03-28"})
    void paques(int annee, LocalDate attendu) {
        assertThat(JoursOuvres.paques(annee)).isEqualTo(attendu);
    }

    @ParameterizedTest
    @CsvSource({"2026-01-01", "2026-04-06", "2026-05-01", "2026-05-08", "2026-05-14", "2026-05-25", "2026-07-14",
        "2026-08-15", "2026-11-01", "2026-11-11", "2026-12-25"})
    void lesOnzeJoursFeriesNationaux(LocalDate jour) {
        assertThat(JoursOuvres.estFerie(jour)).isTrue();
        assertThat(JoursOuvres.estOuvre(jour)).isFalse();
    }

    @Test
    void lundiDePaquesEtNoelSontReconnusCommeFeriesMemeEnSemaine() {
        // Lundi de Pâques 2026 (06/04) et 25/12/2026 (un vendredi) : jours de semaine, mais fériés.
        LocalDate lundiDePaques = LocalDate.of(2026, 4, 6);
        LocalDate noel = LocalDate.of(2026, 12, 25);
        assertThat(lundiDePaques.getDayOfWeek()).isEqualTo(java.time.DayOfWeek.MONDAY);
        assertThat(noel.getDayOfWeek()).isEqualTo(java.time.DayOfWeek.FRIDAY);
        assertThat(JoursOuvres.estFerie(lundiDePaques)).isTrue();
        assertThat(JoursOuvres.estFerie(noel)).isTrue();
        assertThat(JoursOuvres.estOuvre(lundiDePaques)).isFalse();
        assertThat(JoursOuvres.estOuvre(noel)).isFalse();
        // Les veilles et lendemains ordinaires restent ouvrés.
        assertThat(JoursOuvres.estOuvre(LocalDate.of(2026, 4, 7))).isTrue();
        assertThat(JoursOuvres.estOuvre(LocalDate.of(2026, 12, 24))).isTrue();
    }

    @Test
    void unJourOrdinaireEstOuvreSaufLeWeekEnd() {
        assertThat(JoursOuvres.estOuvre(LocalDate.of(2026, 11, 26))).isTrue();
        assertThat(JoursOuvres.estOuvre(LocalDate.of(2026, 11, 28))).isFalse();
        assertThat(JoursOuvres.estOuvre(LocalDate.of(2026, 11, 29))).isFalse();
    }

    @Test
    void cinqJoursOuvresSautentLeWeekEnd() {
        // Jeudi 19/11/2026 : ven 20, lun 23, mar 24, mer 25, jeu 26.
        assertThat(JoursOuvres.ajouter(LocalDate.of(2026, 11, 19), 5)).isEqualTo(LocalDate.of(2026, 11, 26));
    }

    @Test
    void lesJoursFeriesNeComptentPas() {
        // Lundi 09/11/2026 : mar 10, (mer 11 férié), jeu 12, ven 13, lun 16, mar 17.
        assertThat(JoursOuvres.ajouter(LocalDate.of(2026, 11, 9), 5)).isEqualTo(LocalDate.of(2026, 11, 17));
        // Vol : 2 jours ouvrés après le jeudi 24/12/2026 : (ven 25 férié) lun 28, mar 29.
        assertThat(JoursOuvres.ajouter(LocalDate.of(2026, 12, 24), 2)).isEqualTo(LocalDate.of(2026, 12, 29));
    }
}
