package net.checkconsulting.assuranceiard.api.dto;

import java.time.Instant;

/** US-04, RG3 : une ligne du journal des accès refusés. */
public record AccesRefuseResponse(
        Instant date,
        String email,
        String profil,
        String methode,
        String chemin,
        String ressource,
        String identifiant,
        String rubrique,
        String droit,
        String motif) {
}
