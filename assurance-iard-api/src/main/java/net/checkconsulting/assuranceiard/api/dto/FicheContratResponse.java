package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * US-18, AC3 : fiche d'un contrat habitation ou auto. RGPD : l'IBAN n'est connu que masqué ; aucune donnée du
 * conducteur (date de naissance, permis, bonus-malus) ni de santé.
 */
public record FicheContratResponse(
        String numero,
        UUID referenceDevis,
        String produit,
        String statut,
        String formule,
        Logement logement,
        Vehicule vehicule,
        BigDecimal franchise,
        List<Garantie> garanties,
        BigDecimal cotisationMensuelle,
        BigDecimal cotisationAnnuelle,
        LocalDate dateEffet,
        LocalDate dateEcheance,
        LocalDate dateFin,
        Paiement paiement,
        List<Document> documents,
        List<Version> versionsConditionsParticulieres) {

    public record Logement(String type, Integer surface, String adresse, String codePostal, String ville) {
    }

    public record Vehicule(String marque, String modele, String version, String immatriculation) {
    }

    /** Option souscrite ; prix inconnu pour une option reprise du devis (contrats du sprint 1). */
    public record Garantie(String code, BigDecimal prixMensuelTtc) {
    }

    public record Paiement(String frequence, Integer jourPrelevement, String ibanMasque, String rum) {
    }

    /** Document en vigueur, téléchargeable par /documents-contrat/{id}. */
    public record Document(Long id, String type, String libelle, Integer version, Instant dateCreation) {
    }

    /**
     * US-21, AC2 : une version des conditions particulières (EN_VIGUEUR ou REMPLACE), la plus récente d'abord.
     *
     * @param disponible vrai si son PDF est stocké (sinon le téléchargement répond « en préparation »)
     */
    public record Version(Long id, Integer version, String statut, Instant dateCreation, String motif,
            boolean disponible) {
    }
}
