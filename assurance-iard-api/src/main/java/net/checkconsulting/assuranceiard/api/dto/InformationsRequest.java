package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InformationsRequest(
        @NotNull UUID referenceDevis,
        @NotNull @Pattern(regexp = "madame|monsieur") String civilite,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotNull LocalDate dateNaissance,
        @NotNull @Pattern(regexp = "0[1-9]\\d{8}") String telephone,
        @NotNull @Pattern(regexp = "carte-identite|passeport|titre-sejour") String typePieceIdentite,
        @NotNull @Pattern(regexp = "electricite|gaz|internet|quittance-loyer") String typeJustificatifDomicile,
        @NotNull @Size(max = 9) List<@NotBlank @Size(max = 200) String> colocataires) {
}
