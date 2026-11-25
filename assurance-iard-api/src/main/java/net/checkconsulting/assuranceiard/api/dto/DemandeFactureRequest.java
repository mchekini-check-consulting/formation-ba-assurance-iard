package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * US-22, AC2 : facture demandée pour une échéance (sa date) ou pour une année de cotisation (sa date de début). Le
 * montant n'est jamais envoyé par l'écran : il est relu dans l'échéancier du contrat.
 */
public record DemandeFactureRequest(@NotNull Periode periode, @NotNull LocalDate date) {

    public enum Periode {
        ECHEANCE,
        ANNEE
    }
}
