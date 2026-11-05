package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Réponses de l'étape 1, dans la forme utilisée par l'écran. */
public record LogementDevis(
        @Size(max = 30) String statut,
        @Size(max = 12) String residence,
        @NotNull @Pattern(regexp = "appartement|maison") String typeLogement,
        @NotBlank @Size(max = 255) String adresse,
        @NotNull @Pattern(regexp = "\\d{5}") String codePostal,
        @NotBlank @Size(max = 100) String ville,
        @NotNull @Pattern(regexp = "9|[1-9]\\d{1,2}|1000") String surface,
        @Size(max = 5) String nombrePieces,
        @Size(max = 30) String antecedent,
        @Size(max = 30) String resilieContrat,
        @Size(max = 30) String assuranceAntecedent,
        @Size(max = 30) String etage,
        Boolean dependances,
        @Size(max = 5) String nombreColocataires,
        @Size(max = 100) String assureurActuel,
        @Size(max = 5) String nombreSinistres) {
}
