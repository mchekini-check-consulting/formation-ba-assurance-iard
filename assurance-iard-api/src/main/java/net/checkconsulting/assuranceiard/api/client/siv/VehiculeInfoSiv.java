package net.checkconsulting.assuranceiard.api.client.siv;

import java.math.BigDecimal;

/** US-27 : informations véhicule retournées par le simulateur SIV. */
public record VehiculeInfoSiv(
        Long refVehiculeId,
        String marque,
        String modele,
        String energie,
        String version,
        Integer annee,
        BigDecimal valeurNeuve) {
}
