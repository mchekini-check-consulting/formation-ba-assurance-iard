package net.checkconsulting.assuranceiard.api.exception;

/** US-21, RG2 : le contrat est Suspendu ou pas encore signé : pas de régénération de ses conditions particulières. */
public class RegenerationImpossibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegenerationImpossibleException() {
        super("La régénération des conditions particulières n'est pas possible pour ce contrat dans son statut actuel.");
    }
}
