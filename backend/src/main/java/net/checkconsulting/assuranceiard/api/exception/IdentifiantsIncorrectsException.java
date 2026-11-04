package net.checkconsulting.assuranceiard.api.exception;

public class IdentifiantsIncorrectsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IdentifiantsIncorrectsException() {
        super("E-mail ou mot de passe incorrect.");
    }
}
