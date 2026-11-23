package net.checkconsulting.assuranceiard.api.service.document;

import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;

/**
 * T-03 : point d'extension des documents PDF. Pour ajouter un modèle (facture US-22, avenant US-23, IPID et fiche
 * conseil US-29, mandat SEPA et échéancier US-31, mémo véhicule US-33) : une classe {@code @Component} qui
 * implémente cette interface. Elle est trouvée automatiquement par {@link CatalogueModeles} : le service commun
 * (versions, stockage, reprise, téléchargement) ne change pas.
 */
public interface ModeleDocument {

    /** Type de document produit (colonne document.type). */
    TypeDocument type();

    /** Vrai si ce modèle produit ce document, quand plusieurs modèles ont le même type (ex. CP habitation / auto). */
    default boolean convientA(Document document) {
        return true;
    }

    /**
     * Rédige le document à sa version actuelle (contrat, référence et version sont dans {@code document}).
     * Rien de personnel dans les exceptions : leur message peut être journalisé.
     */
    ContenuDocument rediger(Document document);
}
