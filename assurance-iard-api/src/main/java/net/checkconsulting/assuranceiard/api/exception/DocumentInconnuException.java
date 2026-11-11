package net.checkconsulting.assuranceiard.api.exception;

public class DocumentInconnuException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DocumentInconnuException() {
        super("Document introuvable.");
    }
}
