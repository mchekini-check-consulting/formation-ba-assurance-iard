package net.checkconsulting.assuranceiard.api.exception;

/** T-03 : le PDF stocké ne correspond plus à son empreinte SHA-256 ; il n'est jamais envoyé. */
public class DocumentAltereException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DocumentAltereException() {
        super("Document momentanément indisponible, contactez votre conseiller.");
    }
}
