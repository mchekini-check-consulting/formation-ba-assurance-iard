package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record MandatSignatureRequest(
        @NotNull UUID referenceDevis,
        @NotBlank @Size(max = 140) String titulaire,
        @NotBlank @Size(max = 64) String iban,
        @NotNull LocalDate dateDebut,
        @NotNull Integer jourPrelevement,
        @NotNull @AssertTrue Boolean autorisation) {

    /** L'IBAN ne doit jamais apparaître dans un log, même par accident. */
    @Override
    public String toString() {
        return "MandatSignatureRequest[referenceDevis=" + referenceDevis + "]";
    }
}
