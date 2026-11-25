package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;

/** US-27 : modèle de véhicule dans le référentiel. */
public record ModeleResponse(
        Long id,
        String modele,
        String energie,
        String version,
        Integer annee,
        BigDecimal valeurNeuve) {
}
