package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/** US-07 : l'adresse est acceptée sans tenir compte des espaces autour ni des majuscules. */
public record MotDePasseOublieRequest(@NotBlank @Email @Size(max = 254) String email) {

    public MotDePasseOublieRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
