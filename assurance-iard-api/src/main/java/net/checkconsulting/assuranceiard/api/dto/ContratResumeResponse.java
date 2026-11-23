package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** US-04 : un contrat dans la liste « Mes contrats ». */
public record ContratResumeResponse(
        UUID referenceDevis,
        String numero,
        String statut,
        String adresseLogement,
        String codePostal,
        String ville,
        BigDecimal cotisationMensuelle,
        LocalDate dateEffet) {
}
