package net.checkconsulting.assuranceiard.api.exception;

public class CodeSignatureExpireException extends RuntimeException {

    public CodeSignatureExpireException() {
        super("Votre code a expiré. Demandez un nouveau code.");
    }
}
