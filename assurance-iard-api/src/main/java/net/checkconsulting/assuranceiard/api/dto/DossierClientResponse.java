package net.checkconsulting.assuranceiard.api.dto;

import java.util.List;

/** US-04 : dossier d'un client vu du back-office. */
public record DossierClientResponse(
        Long id,
        String civilite,
        String prenom,
        String nom,
        String email,
        String telephone,
        String profil,
        List<ContratClientResponse> contrats,
        List<DocumentResponse> documents) {
}
