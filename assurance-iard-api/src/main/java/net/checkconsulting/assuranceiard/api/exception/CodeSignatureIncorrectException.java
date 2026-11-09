package net.checkconsulting.assuranceiard.api.exception;

public class CodeSignatureIncorrectException extends RuntimeException {

    public CodeSignatureIncorrectException() {
        super("Code incorrect. Vérifiez le SMS reçu et réessayez.");
    }
}
