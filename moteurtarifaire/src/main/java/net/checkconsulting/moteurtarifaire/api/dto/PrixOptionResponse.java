package net.checkconsulting.moteurtarifaire.api.dto;

import java.math.BigDecimal;
import net.checkconsulting.moteurtarifaire.api.enums.OptionTarifaire;

public record PrixOptionResponse(OptionTarifaire option, BigDecimal prixMensuel) {
}
