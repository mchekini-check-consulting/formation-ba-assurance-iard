package net.checkconsulting.assuranceiard.api.exception;

import java.util.List;
import lombok.Getter;
import net.checkconsulting.assuranceiard.api.dto.ErreurChampMoteur;

@Getter
public class MoteurDonneesInvalidesException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<ErreurChampMoteur> erreurs;

    public MoteurDonneesInvalidesException(List<ErreurChampMoteur> erreurs) {
        super("La demande contient des champs invalides.");
        this.erreurs = erreurs;
    }
}
