package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import net.checkconsulting.assuranceiard.api.utils.ReglesMotDePasse;

public record NouveauMotDePasseRequest(
        @NotBlank String jeton,
        @NotNull @Size(max = ReglesMotDePasse.LONGUEUR_MAX)
        @Pattern(regexp = ReglesMotDePasse.MOTIF, message = ReglesMotDePasse.MESSAGE)
        String motDePasse) {
}
