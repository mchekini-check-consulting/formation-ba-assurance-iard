package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * US-23, AC1 et AC2 : garanties actuelles du contrat et celles qu'on peut ajouter ou retirer, avec leur prix
 * mensuel TTC (grille de T-04 en auto, grille du sprint 1 en habitation).
 *
 * @param prenom prénom du client (jamais son nom)
 * @param avenantEnAttente AC5 : l'avenant préparé et pas encore accepté ; vide sinon
 */
public record GarantiesContratResponse(
        String numero,
        String produit,
        String formule,
        String statut,
        String prenom,
        BigDecimal cotisationMensuelle,
        BigDecimal cotisationAnnuelle,
        String frequencePaiement,
        List<Garantie> garanties,
        ProjetAvenantResponse avenantEnAttente) {

    public GarantiesContratResponse {
        garanties = List.copyOf(garanties);
    }

    /**
     * Une garantie du contrat ou une option qu'on peut ajouter.
     *
     * @param souscrite vrai si elle est dans le contrat aujourd'hui
     * @param obligatoire AC2 : vrai pour une garantie qu'on ne peut pas retirer (responsabilité civile en auto)
     * @param modifiable faux si elle ne peut être ni ajoutée ni retirée en ligne
     * @param raison AC2 : pourquoi elle n'est pas modifiable ; vide si elle l'est
     */
    public record Garantie(String code, String libelle, BigDecimal prixMensuelTtc, boolean souscrite,
            boolean obligatoire, boolean modifiable, String raison) {
    }
}
