package net.checkconsulting.assuranceiard.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TarifAutoRequest(
        /** Plaque de test (simulateur de décodage SIV). Obligatoire si refVehiculeId est absent. */
        String immatriculation,
        /** Identifiant direct dans ref_vehicule. Obligatoire si immatriculation est absent. */
        Long refVehiculeId,
        @NotNull Integer anneePermis,
        @NotNull @DecimalMin("0.50") @DecimalMax("3.50") BigDecimal bonusMalus,
        @NotNull @Min(1) @Max(200000) Integer kmAnnuels,
        @NotNull String usage,
        @NotNull String stationnement,
        String formule,
        List<String> options) {
}
