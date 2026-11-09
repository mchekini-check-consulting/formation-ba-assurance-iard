package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SignatureContratRequest(@NotBlank String code) {
}
