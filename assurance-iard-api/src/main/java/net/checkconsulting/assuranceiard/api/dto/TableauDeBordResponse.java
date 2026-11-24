package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * US-18, AC1 : l'essentiel de l'espace client. Une rubrique que le profil ne peut pas consulter est vide et
 * signalée comme non visible.
 */
public record TableauDeBordResponse(
        String prenom,
        List<ContratResumeResponse> contrats,
        boolean devisVisibles,
        List<Devis> devisEnCours,
        List<Facture> dernieresFactures,
        boolean sinistresVisibles,
        List<Sinistre> sinistres) {

    /** Devis en cours (habitation ou auto), en consultation. */
    public record Devis(String produit, String description, String formule, BigDecimal prixMensuel,
            LocalDate dateFinValidite) {
    }

    /**
     * Facture d'un contrat.
     *
     * @param documentId PDF téléchargeable par /documents-contrat/{id} ; vide tant qu'il n'est pas stocké
     */
    public record Facture(String numero, String numeroContrat, LocalDate periodeDebut, LocalDate periodeFin,
            BigDecimal montantTtc, Long documentId) {
    }

    /** Sinistre déclaré (les brouillons ne sont pas affichés). */
    public record Sinistre(String numero, String numeroContrat, String type, LocalDate dateSurvenance,
            Instant declareLe) {
    }
}
