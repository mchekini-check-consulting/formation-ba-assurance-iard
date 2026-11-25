package net.checkconsulting.assuranceiard.api.service.document;

/**
 * US-21 : pourquoi une version des conditions particulières a été produite. Le code est enregistré dans la colonne
 * existante document.motif ; l'écran l'affiche en français.
 */
public enum MotifRegeneration {
    /** Bouton « Régénérer mes conditions particulières » (US-21). Seul déclencheur soumis aux règles du client. */
    DEMANDE_CLIENT,
    /** Après un avenant accepté (à brancher par US-23). */
    AVENANT,
    /** Après un changement de coordonnées (à brancher par IA-243, Mon compte). */
    COORDONNEES,
    /** Après la publication de nouvelles conditions générales (ticket à venir). */
    CONDITIONS_GENERALES,
    /** Version 1, créée à la signature du contrat habitation (AC4). */
    SIGNATURE;

    /** Un déclenchement automatique n'est soumis ni à « une par minute » ni à « données inchangées ». */
    public boolean demandeDuClient() {
        return this == DEMANDE_CLIENT;
    }
}
