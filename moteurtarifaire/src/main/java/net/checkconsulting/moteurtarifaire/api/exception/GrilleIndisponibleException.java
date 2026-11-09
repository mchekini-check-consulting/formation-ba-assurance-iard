package net.checkconsulting.moteurtarifaire.api.exception;

import java.time.LocalDate;

public class GrilleIndisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GrilleIndisponibleException(LocalDate date) {
        super("Aucune grille tarifaire n'est en vigueur au " + date + ".");
    }
}
