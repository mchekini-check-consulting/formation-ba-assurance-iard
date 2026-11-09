package net.checkconsulting.assuranceiard.api.exception;

public class EmailDejaUtiliseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmailDejaUtiliseException() {
        super("Un compte existe déjà avec cette adresse.");
    }
}
