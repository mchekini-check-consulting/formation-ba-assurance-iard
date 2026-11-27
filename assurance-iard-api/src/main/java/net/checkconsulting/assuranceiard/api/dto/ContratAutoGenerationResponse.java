package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Résultat de la génération du contrat auto (étape 5). */
public record ContratAutoGenerationResponse(
        String numero,
        String statut,
        Long documentMandatSepaId,
        String email,
        String prenom,
        String nom,
        LocalDate premierPrelevement,
        BigDecimal montantMensuel) {
}
