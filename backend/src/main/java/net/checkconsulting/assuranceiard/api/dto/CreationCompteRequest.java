package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreationCompteRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull @Size(max = 72)
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$", message = "8 caractères minimum, une majuscule et un chiffre")
        String motDePasse) {
}
