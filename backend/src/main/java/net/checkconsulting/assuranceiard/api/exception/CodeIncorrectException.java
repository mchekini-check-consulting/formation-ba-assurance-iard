package net.checkconsulting.assuranceiard.api.exception;

import lombok.Getter;

@Getter
public class CodeIncorrectException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int essaisRestants;

    public CodeIncorrectException(int essaisRestants) {
        super("Code incorrect.");
        this.essaisRestants = essaisRestants;
    }
}
