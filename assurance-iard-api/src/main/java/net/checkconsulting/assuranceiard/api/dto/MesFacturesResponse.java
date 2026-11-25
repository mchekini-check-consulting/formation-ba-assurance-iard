package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * US-22 : page « Mes factures » du client connecté.
 *
 * @param contrats contrats signés (habitation et auto), avec leur échéancier et leurs factures.
 * @param aucuneCotisation AC4 : aucune échéance et aucune facture.
 */
public record MesFacturesResponse(List<Contrat> contrats, boolean aucuneCotisation) {

    /** Échéances et années de la plus récente à la plus ancienne (AC1) ; factures déjà générées (AC2). */
    public record Contrat(String numero, String produit, String statut, String bienAssure, List<Echeance> echeances,
            List<Annee> annees, List<Facture> factures) {
    }

    /**
     * @param statut PAYEE, A_VENIR ou IMPAYEE.
     * @param facturable vrai seulement pour une échéance payée (recette), hors période après la fin d'un contrat
     *     Résilié ou Renoncé.
     * @param facture numéro de la facture qui couvre déjà cette échéance (même période, ou facture d'un mois qui
     *     couvre sa date) ; vide sinon : l'écran propose alors son téléchargement au lieu d'en générer une autre.
     */
    public record Echeance(LocalDate date, BigDecimal montantTtc, String statut, LocalDate periodeDebut,
            LocalDate periodeFin, boolean facturable, String facture) {
    }

    /**
     * Année de cotisation (d'une date anniversaire de l'effet à la suivante) ; montant = ses 12 mensualités.
     *
     * @param facturable vrai seulement si l'année est entièrement réglée (recette).
     * @param reglee vrai si toutes ses échéances sont payées.
     * @param facture numéro de la facture déjà générée pour cette année ; vide sinon.
     */
    public record Annee(LocalDate debut, LocalDate fin, BigDecimal montantTtc, boolean facturable, boolean reglee,
            String facture) {
    }

    /** @param documentId PDF téléchargeable par /documents-contrat/{id} ; vide tant qu'il n'est pas stocké. */
    public record Facture(String numero, LocalDate periodeDebut, LocalDate periodeFin, BigDecimal montantHt,
            BigDecimal taxe, BigDecimal montantTtc, Instant genereeLe, Long documentId) {
    }
}
