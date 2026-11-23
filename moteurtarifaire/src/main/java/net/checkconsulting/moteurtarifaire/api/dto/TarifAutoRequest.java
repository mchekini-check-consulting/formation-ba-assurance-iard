package net.checkconsulting.moteurtarifaire.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import net.checkconsulting.moteurtarifaire.api.enums.FormuleAuto;
import net.checkconsulting.moteurtarifaire.api.enums.StationnementAuto;
import net.checkconsulting.moteurtarifaire.api.enums.UsageAuto;

public record TarifAutoRequest(
        /** Année d'obtention du permis (ex. 2018) — détermine l'ancienneté et la surprime novice. */
        @NotNull @Min(1960) @Max(2100) Integer anneePermis,
        /** Coefficient bonus-malus (CRM) entre 0,50 et 3,50. */
        @NotNull @DecimalMin("0.50") @DecimalMax("3.50") BigDecimal bonusMalus,
        /** Kilométrage annuel prévu. */
        @NotNull @Min(1) @Max(200000) Integer kmAnnuels,
        /** Usage principal du véhicule. */
        @NotNull UsageAuto usage,
        /** Mode de stationnement habituel. */
        @NotNull StationnementAuto stationnement,
        /** Valeur à neuf du véhicule en euros (fournie par assurance-iard-api après résolution). */
        @NotNull @Positive BigDecimal valeurVehicule,
        /** Formule choisie (optionnel) : demande le détail du calcul pour cette formule. */
        FormuleAuto formule,
        /** Options sélectionnées (optionnel, utilisé avec formule). */
        List<String> options) {
}
