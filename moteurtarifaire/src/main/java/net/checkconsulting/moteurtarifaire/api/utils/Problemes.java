package net.checkconsulting.moteurtarifaire.api.utils;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class Problemes {

    private static final Map<HttpStatus, String> TITRES = Map.of(
            HttpStatus.BAD_REQUEST, "Demande invalide",
            HttpStatus.NOT_FOUND, "Introuvable",
            HttpStatus.GONE, "Expiré",
            HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne",
            HttpStatus.SERVICE_UNAVAILABLE, "Service indisponible");

    private Problemes() {
    }

    public static ProblemDetail de(HttpStatus statut, String detail) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(statut, detail);
        probleme.setTitle(TITRES.getOrDefault(statut, statut.getReasonPhrase()));
        return probleme;
    }
}
