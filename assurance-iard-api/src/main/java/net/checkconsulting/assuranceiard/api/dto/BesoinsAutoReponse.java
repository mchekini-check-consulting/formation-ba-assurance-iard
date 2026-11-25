package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.util.List;

/** US-29 : recommandation de formule retournée après analyse des besoins. */
public record BesoinsAutoReponse(
        String formuleConseillee,
        List<String> raisons,
        BigDecimal prixMensuel) {
}
