package net.checkconsulting.assuranceiard.api.exception;

import java.time.Instant;
import lombok.Getter;

@Getter
public class CompteBloqueException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Instant bloqueJusquA;

    public CompteBloqueException(Instant bloqueJusquA) {
        super("Compte bloqué 15 minutes.");
        this.bloqueJusquA = bloqueJusquA;
    }
}
