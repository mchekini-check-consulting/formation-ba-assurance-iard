package net.checkconsulting.moteurtarifaire.api.dto;

import java.math.BigDecimal;
import java.util.List;
import net.checkconsulting.moteurtarifaire.api.enums.Formule;

public record DetailFormuleResponse(
        Formule formule,
        BigDecimal prixFormule,
        List<PrixOptionResponse> options,
        BigDecimal ajustementFranchise,
        BigDecimal totalMensuel,
        BigDecimal totalAnnuel) {
}
