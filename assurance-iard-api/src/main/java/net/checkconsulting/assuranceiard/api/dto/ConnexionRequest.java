package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConnexionRequest(@NotBlank String email, @NotNull String motDePasse) {
}
