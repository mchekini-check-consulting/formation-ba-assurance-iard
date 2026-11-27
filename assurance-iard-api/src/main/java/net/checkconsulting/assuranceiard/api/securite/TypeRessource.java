package net.checkconsulting.assuranceiard.api.securite;

/**
 * US-04 : ressources dont le propriétaire est contrôlé. Chaque type (sauf AUCUNE) a son {@link RessourceProtegee}.
 * Sprint 2 : ajouter SINISTRE ici et une classe RessourceProtegee qui retrouve le propriétaire d'un sinistre.
 */
public enum TypeRessource {
    /** La route ne vise pas une ressource précise (liste de mes devis, création...). */
    AUCUNE,
    /** Devis enregistré (US-12), identifié par sa référence. */
    DEVIS,
    /** Dossier de souscription (US-13 à US-16), identifié par la référence du devis. */
    SOUSCRIPTION,
    /** Contrat (US-15), identifié par la référence du devis. */
    CONTRAT,
    /** Contrat habitation ou auto (US-18), identifié par son numéro. */
    CONTRAT_NUMERO,
    /** Document déposé (US-13), identifié par son numéro. */
    DOCUMENT,
    /** Document PDF généré (T-03 : conditions particulières...), identifié par son numéro dans la table document. */
    DOCUMENT_CONTRAT,
    /** Déclaration de sinistre (US-24), identifiée par son identifiant technique ; appartient au client du contrat. */
    SINISTRE
}
