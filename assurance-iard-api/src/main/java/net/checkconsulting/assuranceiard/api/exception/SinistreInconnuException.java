package net.checkconsulting.assuranceiard.api.exception;

/** US-24 : déclaration de sinistre introuvable (ou brouillon expiré, RG6). */
public class SinistreInconnuException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String MESSAGE = "Cette déclaration est introuvable. Un brouillon est conservé 30 jours.";

    public SinistreInconnuException() {
        super(MESSAGE);
    }
}
