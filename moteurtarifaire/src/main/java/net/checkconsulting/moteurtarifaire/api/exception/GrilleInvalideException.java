package net.checkconsulting.moteurtarifaire.api.exception;

public class GrilleInvalideException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GrilleInvalideException(String message) {
        super(message);
    }

    public GrilleInvalideException(String message, Throwable cause) {
        super(message, cause);
    }
}
