package net.checkconsulting.assuranceiard.api.dto;

import java.time.Instant;

/** US-24, AC9 : dossier créé (numéro SIN-AAAA-XXXXXX, statut Déclaré) et accusé de réception envoyé ou non. */
public record SinistreEnvoyeResponse(Long id, String numero, String statut, Instant declareLe,
        boolean piecesManquantes, boolean emailEnvoye) {
}
