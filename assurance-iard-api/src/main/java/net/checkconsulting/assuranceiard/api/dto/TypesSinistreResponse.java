package net.checkconsulting.assuranceiard.api.dto;

import java.util.List;

/** US-24, AC2, AC7, AC10, RG1 : types de sinistres du produit du contrat (« Autre » en dernier), pièces attendues. */
public record TypesSinistreResponse(String numeroContrat, String produit, String numeroAssistance,
        List<Type> types) {

    /**
     * @param delaiPrevu RG2, en toutes lettres (« 5 jours ouvrés »)
     * @param demandeDateArrete RG2 : catastrophe naturelle, la date de l'arrêté est demandée (facultative)
     * @param gestesUrgence AC10 : gestes d'urgence (habitation)
     */
    public record Type(String code, String libelle, String aide, String delaiPrevu, boolean demandeDateArrete,
            List<String> gestesUrgence, List<PieceAttendue> pieces) {
    }

    public record PieceAttendue(String code, String libelle, boolean obligatoire) {
    }
}
