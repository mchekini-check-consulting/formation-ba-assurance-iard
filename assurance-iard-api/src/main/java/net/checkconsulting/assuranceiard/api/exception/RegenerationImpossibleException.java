package net.checkconsulting.assuranceiard.api.exception;

/** US-21, RG2 : le statut du contrat ne permet pas de régénérer ses conditions particulières (RG3 d'US-18). */
public class RegenerationImpossibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegenerationImpossibleException() {
        super("La régénération des conditions particulières n'est pas possible pour ce contrat dans son statut actuel.");
    }
}
