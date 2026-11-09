package net.checkconsulting.moteurtarifaire.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Builder;
import net.checkconsulting.moteurtarifaire.api.utils.LimitesSaisie;
import net.checkconsulting.moteurtarifaire.api.enums.Priorite;
import net.checkconsulting.moteurtarifaire.api.enums.Remboursement;

@Builder(toBuilder = true)
public record BesoinsRequest(
        @NotNull @DecimalMin(LimitesSaisie.CONTENU_MIN) BigDecimal valeurContenu,
        @NotNull Boolean objetsValeur,
        @NotNull Boolean appareilsCouteux,
        @NotNull Remboursement remboursement,
        @NotNull Boolean assistance,
        @NotNull Boolean litiges,
        @NotNull Priorite priorite) {
}
