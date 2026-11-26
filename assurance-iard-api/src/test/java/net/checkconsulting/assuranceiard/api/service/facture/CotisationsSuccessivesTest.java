package net.checkconsulting.assuranceiard.api.service.facture;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.checkconsulting.assuranceiard.api.model.Avenant;
import net.checkconsulting.assuranceiard.api.service.facture.EcheancierService.LigneEcheance;
import org.junit.jupiter.api.Test;

/** US-23, RG3 : la nouvelle cotisation s'applique à partir de la date d'effet de l'avenant, sans prorata. */
class CotisationsSuccessivesTest {

    private static final LocalDate EFFET = LocalDate.of(2026, 11, 27);

    private static Avenant accepte(LocalDate effet, String avant, String apres) {
        Avenant avenant = new Avenant();
        avenant.setStatut("ACCEPTE");
        avenant.setDateEffet(effet);
        avenant.setPrimeAvantTtc(new BigDecimal(avant));
        avenant.setPrimeApresTtc(new BigDecimal(apres));
        return avenant;
    }

    private static LigneEcheance ligne(LocalDate date, String montant, String statut) {
        return new LigneEcheance(date, new BigDecimal(montant), statut, date, date.plusMonths(1).minusDays(1));
    }

    @Test
    void mensuelLaPremiereMensualiteApresLaDateDEffetEstALaNouvelleCotisation() {
        CotisationsSuccessives cotisations = new CotisationsSuccessives(
                List.of(accepte(EFFET, "18.50", "21.00")), new BigDecimal("21.00"), new BigDecimal("252.00"), false);

        List<LigneEcheance> lignes = cotisations.appliquer(List.of(
                ligne(LocalDate.of(2026, 12, 5), "21.00", "A_VENIR"),
                ligne(LocalDate.of(2026, 11, 5), "21.00", "PAYEE")), Map.of());

        assertThat(lignes).extracting(ligne -> ligne.montantTtc().toPlainString()).containsExactly("21.00", "18.50");
    }

    @Test
    void lesEcheancesEnregistreesPayeesGardentLeurMontantLesAVenirSuiventLAvenant() {
        CotisationsSuccessives cotisations = new CotisationsSuccessives(
                List.of(accepte(EFFET, "18.50", "21.00")), new BigDecimal("21.00"), new BigDecimal("252.00"), false);
        LocalDate payee = LocalDate.of(2026, 11, 10);
        LocalDate aVenirAvant = LocalDate.of(2026, 11, 26);
        LocalDate aVenirApres = LocalDate.of(2026, 12, 10);

        List<LigneEcheance> lignes = cotisations.appliquer(List.of(
                ligne(aVenirApres, "18.50", "A_VENIR"), ligne(aVenirAvant, "18.50", "A_VENIR"),
                ligne(payee, "17.00", "PAYEE")),
                Map.of(payee, "PAYEE", aVenirAvant, "A_VENIR", aVenirApres, "A_VENIR"));

        assertThat(lignes).extracting(ligne -> ligne.montantTtc().toPlainString())
                .containsExactly("21.00", "18.50", "17.00");
    }

    @Test
    void annuelLaProchaineEcheanceAnnuelleEstALaNouvelleCotisationAnnuelle() {
        CotisationsSuccessives cotisations = new CotisationsSuccessives(
                List.of(accepte(EFFET, "18.50", "21.00")), new BigDecimal("21.00"), new BigDecimal("252.00"), true);

        assertThat(cotisations.enVigueur(LocalDate.of(2026, 3, 10))).isEqualByComparingTo("222.00");
        assertThat(cotisations.enVigueur(LocalDate.of(2027, 3, 10))).isEqualByComparingTo("252.00");
    }

    @Test
    void plusieursAvenantsSuccessifs() {
        CotisationsSuccessives cotisations = new CotisationsSuccessives(List.of(
                accepte(LocalDate.of(2026, 10, 2), "45.00", "47.50"), accepte(EFFET, "47.50", "48.50")),
                new BigDecimal("48.50"), new BigDecimal("582.00"), false);

        assertThat(cotisations.enVigueur(LocalDate.of(2026, 10, 1))).isEqualByComparingTo("45.00");
        assertThat(cotisations.enVigueur(LocalDate.of(2026, 11, 5))).isEqualByComparingTo("47.50");
        assertThat(cotisations.enVigueur(EFFET)).isEqualByComparingTo("48.50");
    }

    @Test
    void sansAvenantRienNeChange() {
        List<LigneEcheance> lignes = List.of(ligne(EFFET, "18.50", "A_VENIR"));

        assertThat(new CotisationsSuccessives(List.of(), new BigDecimal("21.00"), new BigDecimal("252.00"), false)
                .appliquer(lignes, Map.of())).isSameAs(lignes);
    }
}
