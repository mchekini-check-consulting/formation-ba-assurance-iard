package net.checkconsulting.assuranceiard.api.service.document;

import java.util.Objects;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;

/**
 * T-03 : ce qu'une US demande de générer.
 *
 * @param type type de document.
 * @param contratId contrat concerné ; vide pour un document commun (conditions générales, IPID).
 * @param reference n° de facture ou d'avenant, produit, formule ; vide pour un document unique du contrat.
 * @param motif raison de cette version (100 caractères au plus), ex. « Signature », « Avenant n° 2 ».
 */
public record DemandeDocument(TypeDocument type, Long contratId, String reference, String motif) {

    public DemandeDocument {
        Objects.requireNonNull(type, "type");
        if (contratId == null && reference == null) {
            throw new IllegalArgumentException("Un document sans contrat doit avoir une référence");
        }
    }

    /** Document unique d'un contrat (conditions particulières, mémo véhicule...). */
    public static DemandeDocument duContrat(TypeDocument type, long contratId, String motif) {
        return new DemandeDocument(type, contratId, null, motif);
    }

    /** Document commun, sans contrat (conditions générales d'un produit, IPID d'une formule). */
    public static DemandeDocument commun(TypeDocument type, String reference, String motif) {
        return new DemandeDocument(type, null, reference, motif);
    }
}
