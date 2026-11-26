package net.checkconsulting.assuranceiard.api.dto;

import java.time.Instant;
import java.time.LocalDate;

/** US-24 : un dossier ou un brouillon dans « Mes sinistres » (AC8 : un brouillon se reprend à son étape). */
public record SinistreResumeResponse(Long id, String numero, String statut, String type, String libelleType,
        String numeroContrat, String produit, LocalDate dateSurvenance, int etape, boolean piecesManquantes,
        Instant declareLe, Instant brouillonExpireLe) {
}
