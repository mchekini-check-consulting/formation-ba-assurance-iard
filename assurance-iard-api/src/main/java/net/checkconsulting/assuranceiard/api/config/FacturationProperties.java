package net.checkconsulting.assuranceiard.api.config;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * US-22, RG1 : taux de la taxe sur les conventions d'assurance (TSCA), par garantie. Les contrats d'assurance sont
 * exonérés de TVA. Valeurs PROVISOIRES, À VALIDER PAR LE PO (T-04 ne fournit pas de taux) : elles se changent dans
 * application.yaml, sans toucher au code.
 *
 * @param tauxParProduit taux de la formule (garanties de base), par produit (HABITATION, AUTO) ; ex. 0.09 pour 9 %.
 * @param tauxParGarantie taux d'une option, par son code ; à défaut, celui du produit.
 */
@ConfigurationProperties("assurance.facturation")
public record FacturationProperties(Map<String, BigDecimal> tauxParProduit, Map<String, BigDecimal> tauxParGarantie) {

    public FacturationProperties {
        tauxParProduit = tauxParProduit == null ? Map.of() : Map.copyOf(tauxParProduit);
        tauxParGarantie = tauxParGarantie == null ? Map.of() : Map.copyOf(tauxParGarantie);
    }

    /** Taux de la formule du produit ; erreur si le produit n'a pas de taux (réglage manquant). */
    public BigDecimal tauxDuProduit(String produit) {
        BigDecimal taux = produit == null ? null : tauxParProduit.get(produit);
        if (taux == null) {
            throw new IllegalStateException("Aucun taux de TSCA réglé pour le produit " + produit);
        }
        return taux;
    }

    /** Taux d'une option ; celui du produit si l'option n'a pas de taux propre. */
    public BigDecimal tauxDeLOption(String produit, String codeOption) {
        BigDecimal taux = codeOption == null ? null : tauxParGarantie.get(codeOption);
        return taux == null ? tauxDuProduit(produit) : taux;
    }
}
