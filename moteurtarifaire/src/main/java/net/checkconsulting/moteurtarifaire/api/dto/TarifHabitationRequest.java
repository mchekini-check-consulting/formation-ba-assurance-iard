package net.checkconsulting.moteurtarifaire.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import net.checkconsulting.moteurtarifaire.api.dto.validation.ChampsConditionnels;
import net.checkconsulting.moteurtarifaire.api.enums.Dependances;
import net.checkconsulting.moteurtarifaire.api.enums.Etage;
import net.checkconsulting.moteurtarifaire.api.enums.Formule;
import net.checkconsulting.moteurtarifaire.api.utils.LimitesSaisie;
import net.checkconsulting.moteurtarifaire.api.enums.OptionTarifaire;
import net.checkconsulting.moteurtarifaire.api.enums.Residence;
import net.checkconsulting.moteurtarifaire.api.enums.ResiliationAnterieure;
import net.checkconsulting.moteurtarifaire.api.enums.StatutOccupant;
import net.checkconsulting.moteurtarifaire.api.enums.TypeLogement;

@ChampsConditionnels
@Builder(toBuilder = true)
public record TarifHabitationRequest(
        @NotNull StatutOccupant statut,
        @NotNull Residence residence,
        @NotNull TypeLogement typeLogement,
        @NotNull @Pattern(regexp = LimitesSaisie.FORMAT_CODE_POSTAL, message = "{tarif.codePostal.format}")
        String codePostal,
        @NotNull @DecimalMin(LimitesSaisie.SURFACE_MIN) @DecimalMax(LimitesSaisie.SURFACE_MAX) BigDecimal surface,
        @NotNull @Min(LimitesSaisie.PIECES_MIN) @Max(LimitesSaisie.PIECES_MAX) Integer pieces,
        Etage etage,
        Dependances dependances,
        @NotNull @Min(LimitesSaisie.SINISTRES_MIN) @Max(LimitesSaisie.SINISTRES_MAX) Integer sinistres3ans,
        @NotNull ResiliationAnterieure resiliationAnterieure,
        @Valid BesoinsRequest besoins,
        Formule formule,
        List<@NotNull OptionTarifaire> options,
        Integer franchise) {
}
