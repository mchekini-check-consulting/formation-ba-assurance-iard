package net.checkconsulting.assuranceiard.api.service.sinistre;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.repository.ReferentielSinistresRepository.TypeSinistre;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** US-24 : AC1 (contrats ouverts à la déclaration), AC3 et RG4 (couverture), AC5 (période), AC6 et RG2 (délai). */
class ReglesDeclarationTest {

    static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 26);

    static final TypeSinistre VOL = new TypeSinistre("AUTO", "VOL", "Vol du véhicule", null, 2, null,
            List.of("INTER", "TR"), List.of(), List.of());
    static final TypeSinistre BRIS_GLACE = new TypeSinistre("AUTO", "BRIS_GLACE", "Bris de glace", null, 5, null,
            List.of("INTER", "TR"), List.of("BRIS_GLACE"), List.of());
    static final TypeSinistre AUTRE = new TypeSinistre("AUTO", "AUTRE", "Autre / je ne sais pas", null, 5, null,
            List.of(), List.of(), List.of());
    static final TypeSinistre CATNAT = new TypeSinistre("HABITATION", "CLIMATIQUE_CATNAT", "CatNat", null, 5, 30,
            List.of("essentielle", "confort", "serenite"), List.of(), List.of());
    static final TypeSinistre DEGAT_EAUX = new TypeSinistre("HABITATION", "DEGAT_EAUX", "Dégât des eaux", null, 5,
            null, List.of("essentielle", "confort", "serenite"), List.of(), List.of());

    static Contrat contrat(String statut, LocalDate dateEffet, LocalDate dateFin) {
        Contrat contrat = new Contrat();
        contrat.setStatut(statut);
        contrat.setDateEffet(dateEffet);
        contrat.setDateFin(dateFin);
        return contrat;
    }

    @ParameterizedTest
    @CsvSource({"ACTIF, true", "ACTIF_JUSTIFICATIFS_ATTENDUS, true", "AVENANT_EN_ATTENTE, true", "IMPAYE, true",
        "SUSPENDU, true", "RESILIATION_EN_COURS, true", "EN_ATTENTE_DE_SIGNATURE, false", "EN_ATTENTE_EFFET, false",
        "RENONCE, false"})
    void ac1StatutsOuvertsALaDeclaration(String statut, boolean attendu) {
        assertThat(ReglesDeclaration.declarable(contrat(statut, AUJOURDHUI.minusYears(1), null), AUJOURDHUI))
                .isEqualTo(attendu);
    }

    @Test
    void ac1UnContratResilieLEstEncoreMoinsDe2AnsApresSaDateDeFin() {
        assertThat(ReglesDeclaration.declarable(contrat("RESILIE", null, AUJOURDHUI.minusDays(5)), AUJOURDHUI))
                .isTrue();
        assertThat(ReglesDeclaration.declarable(contrat("RESILIE", null, AUJOURDHUI.minusYears(2).plusDays(1)),
                AUJOURDHUI)).isTrue();
        assertThat(ReglesDeclaration.declarable(contrat("RESILIE", null, AUJOURDHUI.minusYears(2)), AUJOURDHUI))
                .isFalse();
        assertThat(ReglesDeclaration.declarable(contrat("RESILIE", null, null), AUJOURDHUI)).isFalse();
    }

    @Test
    void ac3UnVolNEstPasCouvertParUneFormuleTiers() {
        assertThat(ReglesDeclaration.couvert(VOL, "TIERS", List.of("RC"))).contains(false);
        assertThat(ReglesDeclaration.couvert(VOL, "TR", List.of("RC"))).contains(true);
    }

    @Test
    void rg4UneOptionEnVigueurALaDateDuSinistreCouvreAussi() {
        assertThat(ReglesDeclaration.couvert(BRIS_GLACE, "TIERS", List.of("RC"))).contains(false);
        assertThat(ReglesDeclaration.couvert(BRIS_GLACE, "TIERS", List.of("RC", "BRIS_GLACE"))).contains(true);
    }

    @Test
    void ac3LesCodesSontComparesSansTenirCompteDesMajuscules() {
        assertThat(ReglesDeclaration.couvert(DEGAT_EAUX, "CONFORT", List.of())).contains(true);
        assertThat(ReglesDeclaration.couvert(DEGAT_EAUX, null, List.of())).contains(false);
    }

    @Test
    void rg11JamaisDeMessageDeCouverturePourAutre() {
        assertThat(ReglesDeclaration.couvert(AUTRE, "TIERS", List.of())).isEmpty();
    }

    @Test
    void ac5DateAvantLEffetOuApresLaFinDuContrat() {
        Contrat contrat = contrat("RESILIE", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 10, 31));
        assertThat(ReglesDeclaration.horsPeriode(contrat, LocalDate.of(2026, 1, 9))).isTrue();
        assertThat(ReglesDeclaration.horsPeriode(contrat, LocalDate.of(2026, 1, 10))).isFalse();
        assertThat(ReglesDeclaration.horsPeriode(contrat, LocalDate.of(2026, 10, 31))).isFalse();
        assertThat(ReglesDeclaration.horsPeriode(contrat, LocalDate.of(2026, 11, 1))).isTrue();
        assertThat(ReglesDeclaration.horsPeriode(contrat, null)).isFalse();
    }

    @Test
    void ac6CinqJoursOuvresEtDeuxPourUnVol() {
        LocalDate jeudi = LocalDate.of(2026, 11, 19);
        assertThat(ReglesDeclaration.dateLimite(DEGAT_EAUX, jeudi, null)).contains(LocalDate.of(2026, 11, 26));
        assertThat(ReglesDeclaration.horsDelai(DEGAT_EAUX, jeudi, null, AUJOURDHUI)).isFalse();
        assertThat(ReglesDeclaration.horsDelai(DEGAT_EAUX, jeudi.minusDays(1), null, AUJOURDHUI)).isTrue();
        assertThat(ReglesDeclaration.horsDelai(VOL, LocalDate.of(2026, 11, 23), null, AUJOURDHUI)).isTrue();
        assertThat(ReglesDeclaration.horsDelai(VOL, LocalDate.of(2026, 11, 24), null, AUJOURDHUI)).isFalse();
        assertThat(ReglesDeclaration.delaiPrevu(DEGAT_EAUX)).isEqualTo("5 jours ouvrés");
        assertThat(ReglesDeclaration.delaiPrevu(VOL)).isEqualTo("2 jours ouvrés");
    }

    @Test
    void rg2CatastropheNaturelleTrenteJoursApresLArreteEtRienSiSaDateEstInconnue() {
        LocalDate ancien = AUJOURDHUI.minusMonths(3);
        assertThat(ReglesDeclaration.horsDelai(CATNAT, ancien, null, AUJOURDHUI)).isFalse();
        assertThat(ReglesDeclaration.dateLimite(CATNAT, ancien, null)).isEmpty();
        assertThat(ReglesDeclaration.horsDelai(CATNAT, ancien, AUJOURDHUI.minusDays(30), AUJOURDHUI)).isFalse();
        assertThat(ReglesDeclaration.horsDelai(CATNAT, ancien, AUJOURDHUI.minusDays(31), AUJOURDHUI)).isTrue();
        assertThat(ReglesDeclaration.delaiPrevu(CATNAT)).contains("30 jours après la publication");
    }

    @Test
    void ac6SansDateDuSinistreAucunDelaiNEstCalcule() {
        assertThat(ReglesDeclaration.horsDelai(DEGAT_EAUX, null, null, AUJOURDHUI)).isFalse();
    }
}
