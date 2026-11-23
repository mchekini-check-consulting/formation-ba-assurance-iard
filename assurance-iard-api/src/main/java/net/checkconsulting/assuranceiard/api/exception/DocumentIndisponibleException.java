package net.checkconsulting.assuranceiard.api.exception;

/** T-03 : le document existe mais son PDF n'est pas encore stocké (la tâche de reprise s'en charge). */
public class DocumentIndisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DocumentIndisponibleException() {
        super("Document en cours de préparation, réessayez dans quelques minutes.");
    }
}
