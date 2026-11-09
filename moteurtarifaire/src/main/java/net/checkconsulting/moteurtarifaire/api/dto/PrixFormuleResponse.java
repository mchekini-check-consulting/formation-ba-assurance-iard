package net.checkconsulting.moteurtarifaire.api.dto;

import java.math.BigDecimal;
import net.checkconsulting.moteurtarifaire.api.enums.Formule;

public record PrixFormuleResponse(Formule formule, BigDecimal mensuel, BigDecimal annuel) {
}
