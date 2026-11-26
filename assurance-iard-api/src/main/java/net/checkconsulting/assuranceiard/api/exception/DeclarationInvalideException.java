package net.checkconsulting.assuranceiard.api.exception;

/** US-24 : réponse manquante ou invalide dans la déclaration. Le message, en français, s'affiche tel quel. */
public class DeclarationInvalideException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DeclarationInvalideException(String message) {
        super(message);
    }
}
