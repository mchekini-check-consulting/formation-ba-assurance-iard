package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;

/** US-27 : critères d'éligibilité auto relayés depuis le moteur tarifaire. */
public record CriteresEligibiliteResponse(
        BigDecimal bonusMalusMaxLigne,
        String resiliationBloquante) {
}
