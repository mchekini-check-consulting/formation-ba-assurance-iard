package net.checkconsulting.assuranceiard.api.exception;

public class AccesNonAutoriseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccesNonAutoriseException() {
        super("Accès non autorisé.");
    }
}
