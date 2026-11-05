package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DevisEnregistreResponse(
        UUID reference,
        String formule,
        String typeLogement,
        int surface,
        String adresse,
        String codePostal,
        String ville,
        BigDecimal prixMensuel,
        BigDecimal prixAnnuel,
        LocalDate dateFinValidite,
        boolean expire) {
}
