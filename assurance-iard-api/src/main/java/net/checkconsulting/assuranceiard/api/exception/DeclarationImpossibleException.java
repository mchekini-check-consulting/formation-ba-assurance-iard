package net.checkconsulting.assuranceiard.api.exception;

/**
 * US-24, AC1 : le contrat ne permet pas de déclarer un sinistre en ligne (en attente de signature ou d'effet,
 * renoncé, résilié depuis 2 ans ou plus). Refusé aussi en appel direct.
 */
public class DeclarationImpossibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String MESSAGE = "Ce contrat ne permet pas de déclarer un sinistre en ligne dans sa situation "
            + "actuelle. Appelez notre service d'assistance pour être accompagné.";

    public DeclarationImpossibleException() {
        super(MESSAGE);
    }
}
