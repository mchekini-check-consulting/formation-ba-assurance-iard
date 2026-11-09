package net.checkconsulting.assuranceiard.api.exception;

public class VerificationInconnueException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VerificationInconnueException() {
        super("Cette vérification n'existe plus. Reconnectez-vous.");
    }
}
