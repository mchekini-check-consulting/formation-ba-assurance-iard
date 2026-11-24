package net.checkconsulting.assuranceiard.api.exception;

/** US-21 : une seule régénération par minute et par contrat. */
public class RegenerationTropRapprocheeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegenerationTropRapprocheeException() {
        super("Vos conditions particulières viennent d'être régénérées. Réessayez dans une minute.");
    }
}
