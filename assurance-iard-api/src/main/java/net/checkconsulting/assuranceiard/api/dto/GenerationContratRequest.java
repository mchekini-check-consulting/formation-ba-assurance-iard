package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record GenerationContratRequest(
        @NotNull UUID referenceDevis,
        @NotBlank @Size(max = 255) String adresseLogement,
        @NotBlank @Size(max = 5) String codePostal,
        @NotBlank @Size(max = 100) String ville,
        @NotBlank @Size(max = 50) String typeLogement,
        @NotNull @Min(9) @Max(1000) Integer surface,
        @NotNull @Min(0) Integer franchise,
        @NotNull Boolean resiliationHamon) {
}
