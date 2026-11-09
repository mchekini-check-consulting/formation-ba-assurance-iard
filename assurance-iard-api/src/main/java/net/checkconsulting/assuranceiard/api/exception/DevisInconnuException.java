package net.checkconsulting.assuranceiard.api.exception;

public class DevisInconnuException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DevisInconnuException() {
        super("Devis inconnu.");
    }
}
