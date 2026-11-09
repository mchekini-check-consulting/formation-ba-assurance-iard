package net.checkconsulting.assuranceiard.api.exception;

public class CodeSignatureIncorrectException extends RuntimeException {

    private final int essaisRestants;

    public CodeSignatureIncorrectException() {
        super("Code incorrect.");
        this.essaisRestants = -1;
    }

    public CodeSignatureIncorrectException(int essaisRestants) {
        super("Code incorrect. Il vous reste " + essaisRestants + " essai(s).");
        this.essaisRestants = essaisRestants;
    }

    public int getEssaisRestants() {
        return essaisRestants;
    }
}
