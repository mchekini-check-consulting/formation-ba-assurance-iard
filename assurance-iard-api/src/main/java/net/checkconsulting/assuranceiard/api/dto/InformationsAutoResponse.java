package net.checkconsulting.assuranceiard.api.dto;

import java.time.LocalDate;

/** US-31 étape 4 : pré-remplissage du formulaire « Mes informations » (GET). */
public record InformationsAutoResponse(
        String civilite,
        String nom,
        String prenom,
        LocalDate dateNaissance,
        String telephone,
        String adresse,
        String codePostal,
        String ville) {
}
