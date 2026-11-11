package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** US-04, RG4 : l'IBAN n'est jamais renvoyé, seulement ses 4 derniers caractères (ibanFin). */
public record ContratResponse(
        String numero,
        String statut,
        String civilite,
        String prenom,
        String nom,
        String telephone,
        String adresseLogement,
        String codePostal,
        String ville,
        String typeLogement,
        Integer surface,
        String formule,
        List<String> options,
        Integer franchise,
        BigDecimal cotisationMensuelle,
        BigDecimal cotisationAnnuelle,
        Integer jourPrelevement,
        String ibanFin,
        String rum,
        LocalDate datePremierPrelevement,
        LocalDate dateEffet,
        Boolean resiliationHamon,
        LocalDate dateCreation) {
}
