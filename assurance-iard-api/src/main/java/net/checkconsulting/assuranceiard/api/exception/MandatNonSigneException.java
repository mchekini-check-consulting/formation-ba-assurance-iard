package net.checkconsulting.assuranceiard.api.exception;

public class MandatNonSigneException extends RuntimeException {

    public MandatNonSigneException() {
        super("Le mandat SEPA n'a pas encore été signé.");
    }
}
