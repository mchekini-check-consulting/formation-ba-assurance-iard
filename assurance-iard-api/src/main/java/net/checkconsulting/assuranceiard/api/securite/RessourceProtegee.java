package net.checkconsulting.assuranceiard.api.securite;

import java.util.Set;

/**
 * US-04 : retrouve le(s) propriétaire(s) d'une ressource. Une classe par {@link TypeRessource} ; ajouter une
 * ressource (les sinistres au sprint 2) revient à écrire une classe de ce type, sans toucher au contrôle.
 */
public interface RessourceProtegee {

    TypeRessource type();

    /**
     * Comptes propriétaires de la ressource ; vide si elle n'existe pas (la route répond alors « introuvable »).
     *
     * @param identifiant valeur du paramètre de la route (référence, numéro...)
     */
    Set<Long> proprietaires(String identifiant);
}
