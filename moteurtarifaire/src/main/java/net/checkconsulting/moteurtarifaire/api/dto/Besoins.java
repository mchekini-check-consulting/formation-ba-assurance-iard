package net.checkconsulting.moteurtarifaire.api.dto;

import java.math.BigDecimal;
import lombok.Builder;
import net.checkconsulting.moteurtarifaire.api.enums.Priorite;
import net.checkconsulting.moteurtarifaire.api.enums.Remboursement;

@Builder(toBuilder = true)
public record Besoins(
        BigDecimal valeurContenu,
        boolean objetsValeur,
        boolean appareilsCouteux,
        Remboursement remboursement,
        boolean assistance,
        boolean litiges,
        Priorite priorite) {
}
