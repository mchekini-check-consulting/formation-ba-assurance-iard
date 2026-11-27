package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PaiementAutoRequest(
        @NotNull LocalDate dateDebut,
        @NotBlank @Size(min = 2, max = 140) String titulaire,
        @NotBlank @Size(max = 50) String iban,
        @Size(max = 11) String bic,
        @Size(max = 100) String banque,
        @NotNull Integer jourPrelevement,
        @AssertTrue Boolean autorisationPrelevement) {
}
