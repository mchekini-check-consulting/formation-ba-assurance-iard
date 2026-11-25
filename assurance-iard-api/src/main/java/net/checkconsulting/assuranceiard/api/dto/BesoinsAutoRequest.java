package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** US-29 : besoins de couverture saisis à l'étape 2 du parcours auto. */
public record BesoinsAutoRequest(
        @NotNull @Min(1) Integer valeurVehicule,
        @NotNull Boolean vehiculeIndispensable,
        @NotNull Boolean assistance0km,
        @NotNull Boolean protectionConducteur,
        @NotNull Boolean brisGlace,
        @NotNull Boolean accompagnementLitige,
        @NotBlank String priorite) {
}
