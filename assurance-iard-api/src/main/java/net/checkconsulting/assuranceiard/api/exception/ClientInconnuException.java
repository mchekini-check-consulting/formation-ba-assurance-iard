package net.checkconsulting.assuranceiard.api.exception;

public class ClientInconnuException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ClientInconnuException() {
        super("Client introuvable.");
    }
}
