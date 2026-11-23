package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * T-03 : ce que les conditions particulières affichent d'un contrat (habitation ou auto), lu en une fois.
 * Aucune donnée de santé ; l'IBAN n'est connu que masqué (« •••• 4821 »).
 */
public record DonneesContrat(
        Long id,
        String numero,
        String produit,
        String formule,
        Souscripteur souscripteur,
        Logement logement,
        Vehicule vehicule,
        Conducteur conducteur,
        Tarif tarif,
        Paiement paiement,
        AncienContrat ancienContrat,
        List<Option> options) {

    public static final String HABITATION = "HABITATION";
    public static final String AUTO = "AUTO";

    public record Souscripteur(String civilite, String prenom, String nom) {
    }

    public record Logement(String type, Integer surface, String adresse, String codePostal, String ville) {
    }

    public record Vehicule(String immatriculation, String marque, String modele, String version) {
    }

    public record Conducteur(BigDecimal coefficientBonusMalus, Boolean estSouscripteur) {
    }

    public record Tarif(BigDecimal franchise, BigDecimal cotisationMensuelle, BigDecimal cotisationAnnuelle,
            LocalDate dateEffet, LocalDate dateFin) {
    }

    public record Paiement(String frequence, Integer jourPrelevement, String ibanMasque, String rum) {
    }

    /** Ancien contrat résilié pour le client (loi Hamon). */
    public record AncienContrat(String assureur, String numero) {
    }

    /** Option souscrite ; prix inconnu pour une option reprise du devis (contrats du sprint 1). */
    public record Option(String code, BigDecimal prixMensuelTtc) {
    }
}
