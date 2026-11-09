package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record DemandeTarif(
        @NotNull @Valid LogementDevis logement,
        BesoinsDevis besoins,
        @Pattern(regexp = "essentielle|confort|serenite") String formule,
        List<@NotNull @Pattern(regexp = "objets-valeur|assistance-depannage|protection-juridique") String> options,
        @Pattern(regexp = "150|300|500") String franchise) {
}
