package net.checkconsulting.assuranceiard.api.service.facture;

import static net.checkconsulting.assuranceiard.api.service.facture.ContratsFictifs.contrat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.checkconsulting.assuranceiard.api.config.FacturationProperties;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.service.facture.RepartitionTaxe.Garantie;
import net.checkconsulting.assuranceiard.api.service.facture.RepartitionTaxe.Ligne;
import org.junit.jupiter.api.Test;

class CalculFactureTest {

    static final FacturationProperties REGLAGES = new FacturationProperties(
            Map.of("HABITATION", new BigDecimal("0.09"), "AUTO", new BigDecimal("0.18")),
            Map.of("PROTECTION_JURIDIQUE", new BigDecimal("0.134")));

    private final CalculFacture calcul = new CalculFacture(REGLAGES);

    @Test
    void rg1LaFormulePuisChaqueOptionAvecSonTaux() {
        DonneesContrat auto = contrat("AUTO", LocalDate.of(2026, 3, 1), null, "MENSUEL", 10, "40.00", List.of(
                new DonneesContrat.Option("BRIS_GLACE", new BigDecimal("3.50")),
                new DonneesContrat.Option("PROTECTION_JURIDIQUE", new BigDecimal("5.50")),
                new DonneesContrat.Option("RC", null)));

        List<Garantie> garanties = calcul.garanties(auto);

        // L'option sans prix (contrats du sprint 1) reste dans la formule.
        assertThat(garanties).extracting(Garantie::libelle)
                .containsExactly("Formule Tous risques (garanties de base)", "Bris de glace", "Protection juridique");
        assertThat(garanties).extracting(Garantie::poids).containsExactly(new BigDecimal("31.00"),
                new BigDecimal("3.50"), new BigDecimal("5.50"));
        assertThat(garanties).extracting(Garantie::taux).containsExactly(new BigDecimal("0.18"),
                new BigDecimal("0.18"), new BigDecimal("0.134"));

        RepartitionTaxe.Detail detail = calcul.detail(auto, new BigDecimal("40.00"));
        assertThat(detail.lignes()).extracting(Ligne::montantTtc).containsExactly(new BigDecimal("31.00"),
                new BigDecimal("3.50"), new BigDecimal("5.50"));
        assertThat(detail.montantHt().add(detail.taxe())).isEqualByComparingTo("40.00");
    }

    @Test
    void rg1TauxDuProduitObligatoire() {
        assertThat(REGLAGES.tauxDeLOption("HABITATION", "inconnue")).isEqualByComparingTo("0.09");
        assertThat(REGLAGES.tauxDeLOption("HABITATION", null)).isEqualByComparingTo("0.09");
        assertThatThrownBy(() -> REGLAGES.tauxDuProduit("MOTO")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MOTO");
        assertThatThrownBy(() -> new FacturationProperties(null, null).tauxDuProduit(null))
                .isInstanceOf(IllegalStateException.class);
    }
}
