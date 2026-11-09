package net.checkconsulting.moteurtarifaire.api.exception;

import java.time.LocalDate;
import lombok.Getter;

@Getter
public class TarifExpireException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final LocalDate dateValidite;

    public TarifExpireException(LocalDate dateValidite) {
        super("Ce tarif a expiré le " + dateValidite + " : un nouveau calcul est nécessaire.");
        this.dateValidite = dateValidite;
    }
}
