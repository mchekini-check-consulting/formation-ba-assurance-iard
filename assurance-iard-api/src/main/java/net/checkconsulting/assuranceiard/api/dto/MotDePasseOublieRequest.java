package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MotDePasseOublieRequest(@NotBlank @Email @Size(max = 254) String email) {
}
