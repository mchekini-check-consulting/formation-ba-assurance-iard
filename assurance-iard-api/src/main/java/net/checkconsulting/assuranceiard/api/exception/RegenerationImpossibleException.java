package net.checkconsulting.assuranceiard.api.exception;

/** US-21, RG2 : contrat Suspendu, pas encore signé ou plus en vigueur (Résilié, Renoncé) : pas de nouvelle version. */
public class RegenerationImpossibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegenerationImpossibleException() {
        super("La régénération des conditions particulières n'est pas possible pour ce contrat dans son statut actuel.");
    }
}
