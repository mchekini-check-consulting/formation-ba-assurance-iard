package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import net.checkconsulting.assuranceiard.api.utils.ReglesMotDePasse;

public record CreationCompteRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull @Size(max = ReglesMotDePasse.LONGUEUR_MAX)
        @Pattern(regexp = ReglesMotDePasse.MOTIF, message = ReglesMotDePasse.MESSAGE)
        String motDePasse) {
}
