package net.checkconsulting.moteurtarifaire.api.dto;

import java.math.BigDecimal;

public record AjustementFranchiseResponse(int franchise, BigDecimal ajustement) {
}
