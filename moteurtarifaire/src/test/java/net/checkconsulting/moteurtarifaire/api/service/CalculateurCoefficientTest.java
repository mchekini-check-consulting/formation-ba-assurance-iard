package net.checkconsulting.moteurtarifaire.api.service;

import static net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees.GRILLE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees;
import net.checkconsulting.moteurtarifaire.api.dto.ProfilRisque;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire.CoefficientDepart;
import net.checkconsulting.moteurtarifaire.api.service.critere.CritereTarifaire;
import org.junit.jupiter.api.Test;

class CalculateurCoefficientTest {

    private static final CoefficientDepart REGLES = GRILLE.coefficientDepart();

    private final CalculateurCoefficient sansCritere = new CalculateurCoefficient(List.of());

    private BigDecimal coefficientDepart(BigDecimal surface, int pieces) {
        ProfilRisque profil = JeuxDeDonnees.profilNeutre().toBuilder().surface(surface).pieces(pieces).build();
        return sansCritere.coefficient(profil, GRILLE);
    }

    @Test
    void aLaSurfaceEtAuNombreDePiecesDeReferenceLeCoefficientVautUn() {
        assertThat(coefficientDepart(REGLES.surfaceReference(), REGLES.piecesReference()))
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void uneSurfaceDoubleAjouteUneFoisLaPente() {
        assertThat(coefficientDepart(REGLES.surfaceReference().add(REGLES.surfaceReference()), REGLES.piecesReference()))
                .isEqualByComparingTo(BigDecimal.ONE.add(REGLES.penteSurface()));
    }

    @Test
    void unePetiteSurfaceNeDescendPasSousLeMinimum() {
        assertThat(coefficientDepart(BigDecimal.ONE, REGLES.piecesReference()))
                .isEqualByComparingTo(REGLES.coefficientSurfaceMinimum());
    }

    @Test
    void auDelaDuPlafondLaSurfaceNeCompteDePlus() {
        BigDecimal auPlafond = coefficientDepart(REGLES.surfacePlafond(), REGLES.piecesReference());
        BigDecimal auDela = coefficientDepart(REGLES.surfacePlafond().add(BigDecimal.TEN), REGLES.piecesReference());

        assertThat(auDela).isEqualByComparingTo(auPlafond);
    }

    @Test
    void chaquePieceEnPlusAjouteLIncrement() {
        assertThat(coefficientDepart(REGLES.surfaceReference(), REGLES.piecesReference() + 1))
                .isEqualByComparingTo(BigDecimal.ONE.add(REGLES.incrementParPiece()));
    }

    @Test
    void unePieceEnMoinsRetireLIncrement() {
        assertThat(coefficientDepart(REGLES.surfaceReference(), REGLES.piecesReference() - 1))
                .isEqualByComparingTo(BigDecimal.ONE.subtract(REGLES.incrementParPiece()));
    }

    @Test
    void leMinimumPorteSurLaSurfaceLesPiecesRestentAjoutees() {
        assertThat(coefficientDepart(BigDecimal.ONE, REGLES.piecesReference() - 1))
                .isEqualByComparingTo(REGLES.coefficientSurfaceMinimum().subtract(REGLES.incrementParPiece()));
    }

    @Test
    void chaqueCritereMultiplieLeCoefficient() {
        BigDecimal deux = BigDecimal.TWO;
        BigDecimal trois = BigDecimal.valueOf(3);
        CritereTarifaire critereDouble = (profil, grille) -> deux;
        CritereTarifaire critereTriple = (profil, grille) -> trois;
        ProfilRisque profil = JeuxDeDonnees.profilNeutre();

        BigDecimal resultat = new CalculateurCoefficient(List.of(critereDouble, critereTriple)).coefficient(profil, GRILLE);

        assertThat(resultat).isEqualByComparingTo(sansCritere.coefficient(profil, GRILLE).multiply(deux).multiply(trois));
    }
}
