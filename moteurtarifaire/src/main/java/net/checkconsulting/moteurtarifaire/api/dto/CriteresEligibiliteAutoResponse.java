package net.checkconsulting.moteurtarifaire.api.dto;

import java.math.BigDecimal;

/** US-27 : critères d'éligibilité à la souscription en ligne pour l'assurance auto. */
public record CriteresEligibiliteAutoResponse(
        BigDecimal bonusMalusMaxLigne,
        String resiliationBloquante) {
}
