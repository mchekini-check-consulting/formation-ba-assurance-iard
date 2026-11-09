package net.checkconsulting.assuranceiard.api.exception;

import lombok.Getter;

/** Donnée refusée par une règle de l'US-13 : le champ permet d'afficher le message au bon endroit. */
@Getter
public class InformationRefuseeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String champ;

    public InformationRefuseeException(String champ, String message) {
        super(message);
        this.champ = champ;
    }
}
