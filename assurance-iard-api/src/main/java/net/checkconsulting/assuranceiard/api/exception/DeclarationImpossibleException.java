package net.checkconsulting.assuranceiard.api.exception;

/**
 * US-24, AC1 : le contrat ne permet pas de déclarer un sinistre en ligne (en attente de signature ou d'effet,
 * impayé, suspendu, renoncé, résilié depuis 2 ans ou plus). Refusé aussi en appel direct, et à l'envoi d'un brouillon
 * commencé avant que le contrat change de situation (décision PO du 27/11/2026).
 */
public class DeclarationImpossibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String MESSAGE = "Ce contrat ne permet pas de déclarer un sinistre en ligne dans sa situation "
            + "actuelle. Appelez notre service d'assistance pour être accompagné.";

    static final String MESSAGE_ENVOI = "La situation actuelle de votre contrat ne permet plus d'envoyer cette "
            + "déclaration en ligne. Appelez notre assistance 24h/24 au %s : un conseiller enregistrera votre sinistre "
            + "avec vous.";

    public DeclarationImpossibleException() {
        super(MESSAGE);
    }

    private DeclarationImpossibleException(String message) {
        super(message);
    }

    /** Brouillon qui ne peut plus être envoyé : le message donne le numéro d'assistance 24h/24 (AC10). */
    public static DeclarationImpossibleException aLEnvoi(String numeroAssistance) {
        return new DeclarationImpossibleException(MESSAGE_ENVOI.formatted(numeroAssistance));
    }
}
