package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * US-04 / US-18 : un contrat habitation ou auto dans « Mes contrats » et sur le tableau de bord.
 *
 * @param referenceDevis référence du devis de souscription (habitation du sprint 1) ; vide pour l'auto
 * @param bienAssure le logement (adresse) ou le véhicule (marque, modèle, immatriculation), en une ligne
 * @param typeLogement type du logement (habitation) ; vide pour l'auto
 * @param ville ville du logement (habitation) ; vide pour l'auto
 * @param dateEcheance échéance annuelle ; vide si le contrat se termine avant
 * @param ibanMasque IBAN du mandat, toujours masqué (« •••• 4821 ») ; vide sans mandat
 * @param avenants US-23 : avenants acceptés du contrat, avec leur PDF (« Mes documents ») ; vide sur le tableau de
 *     bord
 */
public record ContratResumeResponse(
        UUID referenceDevis,
        String numero,
        String produit,
        String statut,
        String formule,
        String bienAssure,
        String typeLogement,
        String ville,
        BigDecimal cotisationMensuelle,
        LocalDate dateEffet,
        LocalDate dateEcheance,
        LocalDate dateFin,
        String ibanMasque,
        List<AvenantResponse> avenants) {

    /** Sans avenant (tableau de bord, contrats sans avenant accepté). */
    public ContratResumeResponse(UUID referenceDevis, String numero, String produit, String statut, String formule,
            String bienAssure, String typeLogement, String ville, BigDecimal cotisationMensuelle, LocalDate dateEffet,
            LocalDate dateEcheance, LocalDate dateFin, String ibanMasque) {
        this(referenceDevis, numero, produit, statut, formule, bienAssure, typeLogement, ville, cotisationMensuelle,
                dateEffet, dateEcheance, dateFin, ibanMasque, List.of());
    }
}
