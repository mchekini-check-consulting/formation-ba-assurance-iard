package net.checkconsulting.assuranceiard.api.exception;

import lombok.Getter;

@Getter
public class NouveauCodeRequisException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Valeur renvoyée à l'écran pour choisir le message. */
    private final String motif;

    private NouveauCodeRequisException(String message, String motif) {
        super(message);
        this.motif = motif;
    }

    public static NouveauCodeRequisException essaisEpuises() {
        return new NouveauCodeRequisException("Code incorrect. Demandez un nouveau code.", "essais-epuises");
    }

    public static NouveauCodeRequisException codeExpire() {
        return new NouveauCodeRequisException("Ce code a expiré. Demandez un nouveau code.", "code-expire");
    }
}
