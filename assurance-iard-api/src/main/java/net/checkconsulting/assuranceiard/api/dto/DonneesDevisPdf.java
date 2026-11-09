package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DonneesDevisPdf(
        LogementDevis logement,
        String formule,
        List<String> options,
        String franchise,
        BigDecimal prixMensuel,
        BigDecimal prixAnnuel,
        LocalDate dateFinValidite) {
}
