package net.checkconsulting.assuranceiard.api.exception;

/** US-18 : aucun contrat ne porte ce numéro. */
public class ContratInconnuException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ContratInconnuException() {
        super("Contrat inconnu.");
    }
}
