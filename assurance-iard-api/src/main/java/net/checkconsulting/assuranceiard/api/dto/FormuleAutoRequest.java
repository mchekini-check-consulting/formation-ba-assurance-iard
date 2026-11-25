package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

/** US-29 étape 3 : formule et options choisies par le client. */
public record FormuleAutoRequest(
        @NotBlank String formuleChoisie,
        List<String> optionsChoisies,
        Boolean choixDifferentDuConseil,
        String versionGrille,
        BigDecimal prixMensuelTtc,
        BigDecimal prixAnnuelTtc) {
}
