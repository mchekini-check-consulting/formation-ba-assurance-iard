package net.checkconsulting.assuranceiard.api.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.dto.ErreurChampMoteur;
import net.checkconsulting.assuranceiard.api.exception.MoteurDonneesInvalidesException;
import net.checkconsulting.assuranceiard.api.exception.MoteurIndisponibleException;
import net.checkconsulting.assuranceiard.api.exception.MoteurTarifExpireException;
import net.checkconsulting.assuranceiard.api.exception.MoteurTarifInconnuException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@RequiredArgsConstructor
public class MoteurTarifaireErreurDecoder implements ErrorDecoder {

    private static final int DEMANDE_INVALIDE = 400;
    private static final int INCONNU = 404;
    private static final int EXPIRE = 410;

    private final JsonMapper jsonMapper;

    @Override
    public Exception decode(String methode, Response reponse) {
        log.warn("Le moteur tarifaire a répondu par une erreur : statut={}", reponse.status());
        return switch (reponse.status()) {
            case DEMANDE_INVALIDE -> new MoteurDonneesInvalidesException(lireErreurs(reponse));
            case INCONNU -> new MoteurTarifInconnuException();
            case EXPIRE -> new MoteurTarifExpireException();
            default -> new MoteurIndisponibleException();
        };
    }

    private List<ErreurChampMoteur> lireErreurs(Response reponse) {
        if (reponse.body() == null) {
            return List.of();
        }
        try (InputStream corps = reponse.body().asInputStream()) {
            CorpsErreur erreur = jsonMapper.readValue(corps, CorpsErreur.class);
            return erreur.erreurs() == null ? List.of() : erreur.erreurs();
        } catch (IOException | JacksonException illisible) {
            return List.of();
        }
    }

    private record CorpsErreur(List<ErreurChampMoteur> erreurs) {
    }
}
