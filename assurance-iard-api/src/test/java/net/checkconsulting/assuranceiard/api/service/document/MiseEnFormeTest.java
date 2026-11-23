package net.checkconsulting.assuranceiard.api.service.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MiseEnFormeTest {

    @Test
    void lesMontantsSontEnEurosAvecUneVirgule() {
        assertThat(MiseEnForme.montant(new BigDecimal("14.9"))).isEqualTo("14,90 €");
        assertThat(MiseEnForme.montant(new BigDecimal("1234.5"))).isEqualTo("1 234,50 €");
        assertThat(MiseEnForme.montant(BigDecimal.ZERO)).isEqualTo("0,00 €");
    }

    @Test
    void lesDates() {
        assertThat(MiseEnForme.date(LocalDate.of(2026, 1, 5))).isEqualTo("05/01/2026");
        assertThat(MiseEnForme.date(null)).isEqualTo("non renseignée");
        assertThat(MiseEnForme.jourEtMois(LocalDate.of(2026, 3, 9))).isEqualTo("09/03");
    }

    @Test
    void lesLibellesConnusEtInconnus() {
        assertThat(MiseEnForme.formule("serenite")).isEqualTo("Sérénité");
        assertThat(MiseEnForme.formule("TR")).isEqualTo("Tous risques");
        assertThat(MiseEnForme.formule("NOUVELLE")).isEqualTo("NOUVELLE");
        assertThat(MiseEnForme.formule(null)).isEqualTo("non renseignée");
        assertThat(MiseEnForme.option("BRIS_GLACE")).isEqualTo("Bris de glace");
        assertThat(MiseEnForme.option("AUTRE")).isEqualTo("AUTRE");
    }

    @Test
    void laPersonne() {
        assertThat(MiseEnForme.personne("madame", "Camille", "Durand")).isEqualTo("Madame Camille Durand");
        assertThat(MiseEnForme.personne("monsieur", null, "Leroy")).isEqualTo("Monsieur Leroy");
        assertThat(MiseEnForme.personne(null, "Alice", null)).isEqualTo("Alice");
        assertThat(MiseEnForme.personne(null, null, null)).isEqualTo("non renseigné");
    }
}
