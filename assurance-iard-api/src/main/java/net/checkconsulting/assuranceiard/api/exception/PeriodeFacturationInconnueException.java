package net.checkconsulting.assuranceiard.api.exception;

/** US-22 : la date demandée n'est ni une échéance ni le début d'une année de cotisation du contrat. */
public class PeriodeFacturationInconnueException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PeriodeFacturationInconnueException() {
        super("Aucune échéance ni année de cotisation ne correspond à cette date.");
    }
}
