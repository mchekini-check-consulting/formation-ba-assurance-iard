package net.checkconsulting.assuranceiard.api.exception;

public class MoteurIndisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String MESSAGE = "Le calcul du tarif est momentanément indisponible. Veuillez réessayer.";

    public MoteurIndisponibleException() {
        super(MESSAGE);
    }
}
