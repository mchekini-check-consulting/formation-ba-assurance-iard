package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * US-04 / US-18 : un contrat habitation ou auto dans « Mes contrats » et sur le tableau de bord.
 *
 * @param referenceDevis référence du devis de souscription (habitation du sprint 1) ; vide pour l'auto
 * @param bienAssure le logement (adresse) ou le véhicule (marque, modèle, immatriculation), en une ligne
 * @param dateEcheance échéance annuelle ; vide si le contrat se termine avant
 */
public record ContratResumeResponse(
        UUID referenceDevis,
        String numero,
        String produit,
        String statut,
        String bienAssure,
        BigDecimal cotisationMensuelle,
        LocalDate dateEffet,
        LocalDate dateEcheance,
        LocalDate dateFin) {
}
