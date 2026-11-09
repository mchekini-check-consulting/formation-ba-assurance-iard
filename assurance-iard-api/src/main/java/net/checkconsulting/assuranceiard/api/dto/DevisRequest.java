package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Le prix n'en fait pas partie : le backend le calcule lui-même. */
public record DevisRequest(
        @NotNull UUID reference,
        @NotNull @Valid LogementDevis logement,
        @NotNull @Pattern(regexp = "essentielle|confort|serenite") String formule,
        @NotNull @Size(max = 3)
        List<@NotNull @Pattern(regexp = "objets-valeur|assistance-depannage|protection-juridique") String> options,
        @NotNull @Pattern(regexp = "150|300|500") String franchise) {
}
