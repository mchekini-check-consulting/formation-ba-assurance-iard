package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** US-31 étape 4 : saisie des informations du souscripteur auto (POST). */
public record InformationsAutoRequest(
        @NotNull @Pattern(regexp = "madame|monsieur") String civilite,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotNull LocalDate dateNaissance,
        @NotNull @Pattern(regexp = "0[67]\\d{8}") String telephone,
        @NotBlank @Size(min = 5, max = 255) String adresse,
        @NotNull @Pattern(regexp = "\\d{5}") String codePostal,
        @NotBlank @Size(min = 2, max = 100) String ville) {
}
