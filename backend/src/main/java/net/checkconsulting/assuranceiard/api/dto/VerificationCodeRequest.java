package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificationCodeRequest(@NotBlank String verification, @Pattern(regexp = "\\d{6}") @NotBlank String code) {
}
