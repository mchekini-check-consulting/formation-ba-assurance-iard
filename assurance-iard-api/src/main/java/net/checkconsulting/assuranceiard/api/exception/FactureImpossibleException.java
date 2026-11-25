package net.checkconsulting.assuranceiard.api.exception;

/**
 * US-22 : pas de facture pour un contrat en attente de signature, ni, pour un contrat Résilié ou Renoncé, pour une
 * période qui commence à sa date de fin ou après.
 */
public class FactureImpossibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FactureImpossibleException() {
        super("Aucune facture ne peut être générée pour cette période de ce contrat.");
    }
}
