package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * US-04 : un contrat et ses informations de paiement dans le dossier client. AC4/RG4 : de l'IBAN, seuls les
 * 4 derniers caractères (ibanFin) sortent du serveur.
 */
public record ContratClientResponse(
        UUID referenceDevis,
        String numero,
        String statut,
        String adresseLogement,
        String codePostal,
        String ville,
        BigDecimal cotisationMensuelle,
        LocalDate dateEffet,
        String titulaire,
        String ibanFin,
        Integer jourPrelevement,
        String rum) {
}
