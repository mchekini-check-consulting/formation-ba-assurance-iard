package net.checkconsulting.assuranceiard.api.exception;

public class MoteurTarifInconnuException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MoteurTarifInconnuException() {
        super("Tarif inconnu.");
    }
}
