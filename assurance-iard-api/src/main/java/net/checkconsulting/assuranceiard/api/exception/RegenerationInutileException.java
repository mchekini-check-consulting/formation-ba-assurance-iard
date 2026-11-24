package net.checkconsulting.assuranceiard.api.exception;

/** US-21 : le client demande une régénération alors qu'aucune donnée des conditions particulières n'a changé. */
public class RegenerationInutileException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegenerationInutileException(int version) {
        super("Vos conditions particulières sont déjà à jour (version " + version
                + ") : aucune information n'a changé depuis.");
    }
}
