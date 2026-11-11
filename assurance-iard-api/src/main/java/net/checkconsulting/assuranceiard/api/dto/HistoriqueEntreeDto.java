package net.checkconsulting.assuranceiard.api.dto;

import java.time.Instant;

public record HistoriqueEntreeDto(
        Instant modifieLe,
        String rubrique,
        String droit,
        boolean ancienneValeur,
        boolean nouvelleValeur
) {
}
