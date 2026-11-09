package net.checkconsulting.assuranceiard.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TarifRequest(
        String statut,
        String residence,
        String typeLogement,
        String codePostal,
        BigDecimal surface,
        Integer pieces,
        String etage,
        String dependances,
        Integer sinistres3ans,
        String resiliationAnterieure,
        Besoins besoins,
        String formule,
        List<String> options,
        Integer franchise) {

    public record Besoins(
            BigDecimal valeurContenu,
            Boolean objetsValeur,
            Boolean appareilsCouteux,
            String remboursement,
            Boolean assistance,
            Boolean litiges,
            String priorite) {
    }
}
