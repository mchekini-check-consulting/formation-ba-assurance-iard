package net.checkconsulting.assuranceiard.api.exception;

public class TarifDevisIncoherentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TarifDevisIncoherentException() {
        super("Le logement ou la franchise ne correspondent plus au tarif calculé : un nouveau calcul est nécessaire.");
    }
}
