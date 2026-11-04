package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RecommandationRequest(
        @NotNull @Min(0) @Max(9_999_999) Integer valeurContenu,
        @NotNull Boolean objetsValeur,
        @NotNull Boolean appareilsCouteux,
        @NotNull @Pattern(regexp = "neuf|vetuste") String remboursement,
        @NotNull Boolean assistance,
        @NotNull Boolean litiges,
        @NotNull @Pattern(regexp = "prix|equilibre|protection") String priorite,
        @NotNull @Pattern(regexp = "essentielle|confort|serenite") String formule,
        @NotEmpty @Size(max = 4) List<@NotBlank @Size(max = 255) String> raisons,
        @NotNull @Size(max = 3)
        List<@NotNull @Pattern(regexp = "objets-valeur|assistance-depannage|protection-juridique") String> options) {
}
