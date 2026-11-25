package net.checkconsulting.assuranceiard.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/** US-27 : résultat de la recherche par plaque SIV. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SivRechercheResponse(
        String statut,
        String immatriculation,
        Long refVehiculeId,
        String marque,
        String modele,
        String energie,
        String version,
        Integer annee,
        BigDecimal valeurNeuve) {
}
