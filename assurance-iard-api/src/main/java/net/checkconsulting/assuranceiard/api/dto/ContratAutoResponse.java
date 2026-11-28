package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** US-33 : données du contrat auto pour l'écran de signature (étape 6) et l'écran de confirmation. */
public record ContratAutoResponse(
        String numero,
        String statut,
        // Souscripteur
        String civilite,
        String prenom,
        String nom,
        // Véhicule
        String vehiculeMarque,
        String vehiculeModele,
        String vehiculeVersion,
        String vehiculeImmatriculation,
        // Conducteur
        BigDecimal coefficientBonusMalus,
        String nomConducteur,
        // Formule / options / tarif
        String formule,
        List<String> options,
        BigDecimal franchise,
        BigDecimal cotisationMensuelle,
        BigDecimal cotisationAnnuelle,
        LocalDate dateEffet,
        // Paiement
        Integer jourPrelevement,
        String ibanMasque,
        String rum,
        // Hamon
        String ancienAssureur,
        String ancienNumeroContrat,
        // Liens documents (null si non encore créé)
        Long conditionsParticulieresId,
        Long memoVehiculeId,
        Long mandatSepaId,
        Long conditionsGeneralesId,
        Long ipidId,
        Long ficheConseilId,
        // Justificatifs à fournir (liste métier fixe pour l'auto)
        List<String> justificatifsAttendus,
        // Principales exclusions de la formule (textes de l'encadré)
        List<String> exclusions) {
}
