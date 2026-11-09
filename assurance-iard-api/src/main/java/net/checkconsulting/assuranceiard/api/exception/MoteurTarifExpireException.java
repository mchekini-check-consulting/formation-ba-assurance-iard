package net.checkconsulting.assuranceiard.api.exception;

public class MoteurTarifExpireException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MoteurTarifExpireException() {
        super("Ce tarif a expiré : un nouveau calcul est nécessaire.");
    }
}
