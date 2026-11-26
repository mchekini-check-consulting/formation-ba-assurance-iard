package net.checkconsulting.assuranceiard.api.service.avenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import net.checkconsulting.assuranceiard.api.exception.AvenantInvalideException;
import org.junit.jupiter.api.Test;

/** US-23, AC2 et AC3 : nouvelle cotisation = ancienne + ajouts − retraits, avec les prix affichés (AC1). */
class CalculAvenantTest {

    /** Camille, AE-RECETTE-A03 (Tous risques) : RC obligatoire, protection juridique souscrite, options ajoutables. */
    private static final List<GarantieProposee> AUTO = List.of(
            new GarantieProposee("RC", "Responsabilité civile", new BigDecimal("12.00"), true, true,
                    CatalogueGaranties.RAISON_RESPONSABILITE_CIVILE),
            new GarantieProposee("PROTECTION_JURIDIQUE", "Protection juridique", new BigDecimal("2.50"), true, false,
                    null),
            new GarantieProposee("ASSISTANCE_0KM", "Assistance dès 0 km", new BigDecimal("3.50"), false, false, null),
            new GarantieProposee("VEHICULE_REMPLACEMENT", "Véhicule de remplacement", new BigDecimal("4.00"), false,
                    false, null));

    /** Camille, AE-RECETTE-H01 (habitation, Confort) : aucune option, 3 options de la grille du sprint 1. */
    private static final List<GarantieProposee> HABITATION = List.of(
            new GarantieProposee("objets-valeur", "Objets de valeur", new BigDecimal("2.50"), false, false, null),
            new GarantieProposee("assistance-depannage", "Assistance dépannage", new BigDecimal("1.50"), false, false,
                    null),
            new GarantieProposee("protection-juridique", "Protection juridique", new BigDecimal("2.90"), false, false,
                    null));

    @Test
    void ac3AjoutDUneOptionHabitation1850Plus250Egal2100() {
        CalculAvenant.Changement changement = CalculAvenant.calculer(HABITATION, List.of("objets-valeur"),
                new BigDecimal("18.50"));

        assertThat(changement.primeAvant()).isEqualByComparingTo("18.50");
        assertThat(changement.primeApres()).isEqualByComparingTo("21.00");
        assertThat(CalculAvenant.annuelle(changement.primeApres())).isEqualByComparingTo("252.00");
        assertThat(changement.ajouts()).extracting(CalculAvenant.Ligne::code).containsExactly("objets-valeur");
        assertThat(changement.retraits()).isEmpty();
        assertThat(changement.reduitLaCouverture()).isFalse();
    }

    @Test
    void ac3AjoutEtRetraitAuto4750Plus350Moins250Egal4850() {
        CalculAvenant.Changement changement = CalculAvenant.calculer(AUTO, List.of("RC", "ASSISTANCE_0KM"),
                new BigDecimal("47.50"));

        assertThat(changement.primeApres()).isEqualByComparingTo("48.50");
        assertThat(CalculAvenant.annuelle(changement.primeApres())).isEqualByComparingTo("582.00");
        assertThat(changement.ajouts()).singleElement().satisfies(ligne -> {
            assertThat(ligne.action()).isEqualTo(CalculAvenant.AJOUT);
            // Même prix que celui affiché à l'écran (AC1).
            assertThat(ligne.prixMensuelTtc()).isEqualByComparingTo("3.50");
        });
        assertThat(changement.retraits()).singleElement().satisfies(ligne -> {
            assertThat(ligne.code()).isEqualTo("PROTECTION_JURIDIQUE");
            assertThat(ligne.action()).isEqualTo(CalculAvenant.RETRAIT);
        });
        // RG4 : la couverture diminue.
        assertThat(changement.reduitLaCouverture()).isTrue();
        assertThat(changement.lignes()).hasSize(2);
    }

    @Test
    void ac2LaResponsabiliteCivileNeSeRetirePasEtLeMessageEnDitLaRaison() {
        assertThatThrownBy(() -> CalculAvenant.calculer(AUTO, List.of("PROTECTION_JURIDIQUE"),
                new BigDecimal("47.50")))
                .isInstanceOf(AvenantInvalideException.class)
                .hasMessage(CatalogueGaranties.RAISON_RESPONSABILITE_CIVILE)
                .hasMessageContaining("obligatoire");
    }

    @Test
    void continuerRefuseSiAucuneGarantieNAChange() {
        assertThatThrownBy(() -> CalculAvenant.calculer(AUTO, List.of("RC", "PROTECTION_JURIDIQUE"),
                new BigDecimal("47.50")))
                .isInstanceOf(AvenantInvalideException.class)
                .hasMessage(AvenantInvalideException.AUCUN_CHANGEMENT);
    }

    @Test
    void uneGarantieInconnueEstRefusee() {
        assertThatThrownBy(() -> CalculAvenant.calculer(HABITATION, List.of("BRIS_GLACE"), new BigDecimal("18.50")))
                .isInstanceOf(AvenantInvalideException.class)
                .hasMessage(AvenantInvalideException.GARANTIE_INCONNUE);
    }
}
