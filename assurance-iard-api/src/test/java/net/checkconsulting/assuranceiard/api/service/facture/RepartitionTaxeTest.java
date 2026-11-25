package net.checkconsulting.assuranceiard.api.service.facture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import net.checkconsulting.assuranceiard.api.service.facture.RepartitionTaxe.Detail;
import net.checkconsulting.assuranceiard.api.service.facture.RepartitionTaxe.Garantie;
import net.checkconsulting.assuranceiard.api.service.facture.RepartitionTaxe.Ligne;
import org.junit.jupiter.api.Test;

class RepartitionTaxeTest {

    private static final BigDecimal NEUF = new BigDecimal("0.09");
    private static final BigDecimal DIX_HUIT = new BigDecimal("0.18");

    private static BigDecimal euros(String valeur) {
        return new BigDecimal(valeur);
    }

    private static void verifierLesSommes(Detail detail) {
        detail.lignes().forEach(ligne -> assertThat(ligne.montantHt().add(ligne.taxe())).isEqualTo(ligne.montantTtc()));
        assertThat(detail.lignes().stream().map(Ligne::montantTtc).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(detail.montantTtc());
        assertThat(detail.lignes().stream().map(Ligne::montantHt).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(detail.montantHt());
        assertThat(detail.lignes().stream().map(Ligne::taxe).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(detail.taxe());
        assertThat(detail.montantHt().add(detail.taxe())).isEqualTo(detail.montantTtc());
    }

    @Test
    void rg1UneSeuleGarantieHtEtTaxeCalculesDepuisLeTtc() {
        Detail detail = RepartitionTaxe.repartir(euros("18.50"), List.of(new Garantie("Formule", euros("18.50"), NEUF)));

        // 18,50 / 1,09 = 16,97 HT ; 1,53 de taxe (données de recette T-05).
        assertThat(detail.lignes()).singleElement().satisfies(ligne -> {
            assertThat(ligne.montantHt()).isEqualTo(euros("16.97"));
            assertThat(ligne.taxe()).isEqualTo(euros("1.53"));
            assertThat(ligne.taux()).isEqualTo(NEUF);
        });
        verifierLesSommes(detail);
    }

    @Test
    void ac3RepartitAuProrataEtLesCentimesTombentJuste() {
        // 3 garanties de même poids, 10,00 € : 3,34 + 3,33 + 3,33.
        Detail detail = RepartitionTaxe.repartir(euros("10.00"), List.of(
                new Garantie("A", BigDecimal.ONE, NEUF),
                new Garantie("B", BigDecimal.ONE, DIX_HUIT),
                new Garantie("C", BigDecimal.ONE, new BigDecimal("0.134"))));

        assertThat(detail.lignes()).extracting(Ligne::montantTtc)
                .containsExactly(euros("3.34"), euros("3.33"), euros("3.33"));
        assertThat(detail.montantTtc()).isEqualTo(euros("10.00"));
        verifierLesSommes(detail);
    }

    @Test
    void ac3LesTotauxRestentJustesSurUnMontantAnnuel() {
        Detail detail = RepartitionTaxe.repartir(euros("487.37"), List.of(
                new Garantie("Formule", euros("31.07"), DIX_HUIT),
                new Garantie("Bris de glace", euros("3.50"), DIX_HUIT),
                new Garantie("Protection juridique", euros("5.50"), new BigDecimal("0.134"))));

        assertThat(detail.lignes()).hasSize(3);
        verifierLesSommes(detail);
    }

    @Test
    void sansPrixConnuToutVaSurLaFormule() {
        Detail detail = RepartitionTaxe.repartir(euros("20.00"), List.of(
                new Garantie("Formule", BigDecimal.ZERO, NEUF),
                new Garantie("Option sans prix", null, NEUF)));

        assertThat(detail.lignes()).singleElement().extracting(Ligne::libelle).isEqualTo("Formule");
        verifierLesSommes(detail);
    }

    @Test
    void uneGarantieSansPartNEstPasImprimee() {
        Detail detail = RepartitionTaxe.repartir(euros("12.00"), List.of(
                new Garantie("Formule", euros("-2.00"), NEUF),
                new Garantie("Option", euros("12.00"), DIX_HUIT)));

        assertThat(detail.lignes()).extracting(Ligne::libelle).containsExactly("Option");
        verifierLesSommes(detail);
    }

    @Test
    void primeNulleLaFormuleSeuleAZero() {
        Detail detail = RepartitionTaxe.repartir(BigDecimal.ZERO, List.of(
                new Garantie("Formule", euros("10.00"), NEUF),
                new Garantie("Option", euros("2.00"), NEUF)));

        assertThat(detail.lignes()).singleElement().extracting(Ligne::montantTtc).isEqualTo(euros("0.00"));
    }

    @Test
    void auMoinsUneGarantie() {
        assertThatThrownBy(() -> RepartitionTaxe.repartir(BigDecimal.TEN, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
