package net.checkconsulting.assuranceiard.api.exception;

/** US-24, AC16 : le stockage des pièces jointes ne répond pas ; la déclaration reste enregistrée en brouillon. */
public class StockageIndisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String MESSAGE = "Votre pièce n'a pas pu être enregistrée. Réessayez dans quelques instants "
            + "ou envoyez-la plus tard : votre déclaration est conservée et peut être envoyée sans cette pièce.";

    public StockageIndisponibleException() {
        super(MESSAGE);
    }
}
