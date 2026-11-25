package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** US-29 étape 3 : confirmation de l'enregistrement de la formule choisie. */
public record FormuleAutoResponse(
        String formuleChoisie,
        List<String> optionsChoisies,
        BigDecimal prixMensuelTtc,
        BigDecimal prixAnnuelTtc,
        LocalDate dateFinValidite) {
}
