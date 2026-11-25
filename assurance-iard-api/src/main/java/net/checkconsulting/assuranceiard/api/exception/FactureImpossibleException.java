package net.checkconsulting.assuranceiard.api.exception;

/**
 * US-22 : pas de facture pour un contrat en attente de signature, ni, pour un contrat Résilié ou Renoncé, pour une
 * période qui commence à sa date de fin ou après. Recette : ni pour une échéance pas encore payée, ni pour une année
 * d'assurance pas entièrement réglée.
 */
public class FactureImpossibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FactureImpossibleException() {
        this("Aucune facture ne peut être générée pour cette période de ce contrat.");
    }

    /** Recette US-22 : échéance pas encore payée, année pas entièrement réglée. Message sans donnée personnelle. */
    public FactureImpossibleException(String message) {
        super(message);
    }
}
