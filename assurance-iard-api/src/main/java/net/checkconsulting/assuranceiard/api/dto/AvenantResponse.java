package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * US-23 : un avenant du contrat (historique de la fiche, avenant en attente).
 *
 * @param numero numéro AV-AAAA-NNNNNN
 * @param rang rang de l'avenant dans le contrat (1, 2, 3...)
 * @param statut EN_ATTENTE, ACCEPTE ou ABANDONNE
 * @param dateEffet lendemain de l'acceptation ; pour un avenant en attente, le lendemain d'aujourd'hui (s'il est
 *     accepté aujourd'hui)
 * @param ancienneCotisationMensuelle cotisation mensuelle TTC avant l'avenant
 * @param nouvelleCotisationMensuelle cotisation mensuelle TTC après l'avenant
 * @param accepteLe date et heure de l'acceptation ; vide sinon
 * @param documentId PDF de l'avenant (téléchargeable par /documents-contrat/{id}) ; vide avant l'acceptation
 */
public record AvenantResponse(
        String numero,
        int rang,
        String statut,
        LocalDate dateEffet,
        BigDecimal ancienneCotisationMensuelle,
        BigDecimal nouvelleCotisationMensuelle,
        List<Garantie> ajouts,
        List<Garantie> retraits,
        Instant accepteLe,
        Long documentId) {

    public AvenantResponse {
        ajouts = List.copyOf(ajouts);
        retraits = List.copyOf(retraits);
    }

    /** Garantie ajoutée ou retirée, avec son prix mensuel TTC. */
    public record Garantie(String code, String libelle, BigDecimal prixMensuelTtc) {
    }
}
