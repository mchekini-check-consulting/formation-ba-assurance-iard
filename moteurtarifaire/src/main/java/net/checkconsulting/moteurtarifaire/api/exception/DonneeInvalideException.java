package net.checkconsulting.moteurtarifaire.api.exception;

import lombok.Getter;

@Getter
public class DonneeInvalideException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String champ;

    public DonneeInvalideException(String champ, String message) {
        super(message);
        this.champ = champ;
    }
}
