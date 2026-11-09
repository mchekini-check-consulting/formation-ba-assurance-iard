package net.checkconsulting.moteurtarifaire.api.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.moteurtarifaire.api.dto.ProfilRisque;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire.CoefficientDepart;
import net.checkconsulting.moteurtarifaire.api.service.critere.CritereTarifaire;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CalculateurCoefficient {

    private final List<CritereTarifaire> criteres;

    public BigDecimal coefficient(ProfilRisque profil, GrilleTarifaire grille) {
        return criteres.stream()
                .map(critere -> critere.multiplicateur(profil, grille.multiplicateurs()))
                .reduce(coefficientDepart(profil, grille.coefficientDepart()), BigDecimal::multiply);
    }

    private BigDecimal coefficientDepart(ProfilRisque profil, CoefficientDepart regles) {
        return coefficientSurface(profil, regles).add(ajustementPieces(profil, regles));
    }

    private BigDecimal coefficientSurface(ProfilRisque profil, CoefficientDepart regles) {
        BigDecimal surface = profil.surface().min(regles.surfacePlafond());
        BigDecimal ecart = surface.subtract(regles.surfaceReference())
                .multiply(regles.penteSurface())
                .divide(regles.surfaceReference(), MathContext.DECIMAL128);
        return BigDecimal.ONE.add(ecart).max(regles.coefficientSurfaceMinimum());
    }

    private BigDecimal ajustementPieces(ProfilRisque profil, CoefficientDepart regles) {
        return regles.incrementParPiece().multiply(BigDecimal.valueOf((long) profil.pieces() - regles.piecesReference()));
    }
}
