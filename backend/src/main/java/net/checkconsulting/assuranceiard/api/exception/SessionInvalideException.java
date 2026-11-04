package net.checkconsulting.assuranceiard.api.exception;

public class SessionInvalideException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SessionInvalideException() {
        super("Session absente ou expirée.");
    }
}
