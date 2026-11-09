package net.checkconsulting.assuranceiard.api.exception;

public class LienInvalideException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LienInvalideException() {
        super("Ce lien n'est plus valable.");
    }
}
