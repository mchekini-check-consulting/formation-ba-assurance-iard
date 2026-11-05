package net.checkconsulting.assuranceiard.api.dto;

/** Réponses de l'étape 2, gardées avec la recommandation. */
public record BesoinsDevis(
        int valeurContenu,
        boolean objetsValeur,
        boolean appareilsCouteux,
        String remboursement,
        boolean assistance,
        boolean litiges,
        String priorite) {
}
