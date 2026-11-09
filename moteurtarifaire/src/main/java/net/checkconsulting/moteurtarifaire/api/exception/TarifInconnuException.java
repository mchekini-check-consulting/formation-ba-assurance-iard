package net.checkconsulting.moteurtarifaire.api.exception;

import java.util.UUID;

public class TarifInconnuException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TarifInconnuException(UUID idTarif) {
        super("Aucun tarif ne correspond à l'identifiant " + idTarif + ".");
    }
}
