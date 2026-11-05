package net.checkconsulting.assuranceiard.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Même barème provisoire que l'écran (frontend core/model/devis.ts et recommandation.ts).
 * Calculs volontairement en double, comme l'écran, pour obtenir les mêmes arrondis.
 */
@Service
public class TarifService {

    private static final double TARIF_BASE = 7.9;
    private static final double FACTEUR_MAISON = 0.03;
    private static final double FACTEUR_APPARTEMENT = 0.025;
    private static final double DIXIEMES = 10;
    private static final double CENTIEMES = 100;
    private static final int MOIS_PAR_AN = 12;
    private static final int DECIMALES = 2;

    private static final Map<String, Double> COEFFICIENTS =
            Map.of("essentielle", 1.0, "confort", 1.3, "serenite", 1.6);
    private static final Map<String, Double> PRIX_OPTIONS =
            Map.of("objets-valeur", 2.5, "assistance-depannage", 1.5, "protection-juridique", 2.9);
    private static final Map<String, Double> AJUSTEMENTS_FRANCHISE =
            Map.of("150", 1.5, "300", 0.0, "500", -1.0);

    public BigDecimal prixMensuel(
            String typeLogement, int surface, String formule, List<String> options, String franchise) {
        double facteur = "maison".equals(typeLogement) ? FACTEUR_MAISON : FACTEUR_APPARTEMENT;
        double tarif = Math.round((TARIF_BASE + surface * facteur) * DIXIEMES) / DIXIEMES;
        double base = Math.round(tarif * COEFFICIENTS.get(formule) * CENTIEMES) / CENTIEMES;
        double supplements = options.stream().distinct().mapToDouble(PRIX_OPTIONS::get).sum();
        double total = base + supplements + AJUSTEMENTS_FRANCHISE.get(franchise);
        return BigDecimal.valueOf(Math.round(total * CENTIEMES) / CENTIEMES)
                .setScale(DECIMALES, RoundingMode.HALF_UP);
    }

    public BigDecimal prixAnnuel(BigDecimal prixMensuel) {
        return prixMensuel.multiply(BigDecimal.valueOf(MOIS_PAR_AN)).setScale(DECIMALES, RoundingMode.HALF_UP);
    }
}
