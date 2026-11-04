package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RenvoiCodeRequest(@NotBlank String verification) {
}
