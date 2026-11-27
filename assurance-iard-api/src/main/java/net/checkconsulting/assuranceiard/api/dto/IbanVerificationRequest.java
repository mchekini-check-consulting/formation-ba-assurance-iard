package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IbanVerificationRequest(@NotBlank @Size(max = 50) String iban) {
}
