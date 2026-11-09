package net.checkconsulting.moteurtarifaire.api.exception;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.moteurtarifaire.api.dto.ErreurChampResponse;
import net.checkconsulting.moteurtarifaire.api.utils.Problemes;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

@Slf4j
@RestControllerAdvice
public class GlobalControllerAdvice extends ResponseEntityExceptionHandler {

    private static final String PROPRIETE_ERREURS = "erreurs";
    private static final String PROPRIETE_DATE_VALIDITE = "dateValidite";
    private static final String DEMANDE_INVALIDE = "La demande contient des champs invalides.";
    private static final String JSON_ILLISIBLE = "Le corps de la requête n'est pas un JSON valide.";
    private static final String VALEUR_INVALIDE = "La valeur de ce champ n'a pas le bon format.";
    private static final String VALEUR_NON_AUTORISEE = "Valeur non autorisée. Valeurs possibles : ";
    private static final String PARAMETRE_INVALIDE = "Un paramètre de l'adresse n'a pas le bon format.";
    private static final String TARIF_INDISPONIBLE = "Le tarif est momentanément indisponible. Veuillez réessayer plus tard.";
    private static final String ERREUR_INTERNE = "Une erreur interne est survenue. Veuillez réessayer plus tard.";

    @ExceptionHandler(TarifInconnuException.class)
    public ProblemDetail tarifInconnu(TarifInconnuException exception) {
        log.warn("Tarif inconnu demandé");
        return probleme(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(TarifExpireException.class)
    public ProblemDetail tarifExpire(TarifExpireException exception) {
        log.warn("Tarif expiré demandé : dateValidite={}", exception.getDateValidite());
        ProblemDetail probleme = probleme(HttpStatus.GONE, exception);
        probleme.setProperty(PROPRIETE_DATE_VALIDITE, exception.getDateValidite().toString());
        return probleme;
    }

    @ExceptionHandler(DonneeInvalideException.class)
    public ProblemDetail donneeInvalide(DonneeInvalideException exception) {
        journaliserDemandeRefusee(List.of(exception.getChamp()));
        return demandeInvalide(List.of(new ErreurChampResponse(exception.getChamp(), exception.getMessage())));
    }

    @ExceptionHandler(GrilleIndisponibleException.class)
    public ProblemDetail grilleIndisponible(GrilleIndisponibleException exception) {
        log.error(exception.getMessage());
        return probleme(HttpStatus.SERVICE_UNAVAILABLE, exception);
    }

    @ExceptionHandler(GrilleInvalideException.class)
    public ProblemDetail grilleInvalide(GrilleInvalideException exception) {
        log.error("Grille tarifaire invalide : {}", exception.getMessage());
        return Problemes.de(HttpStatus.SERVICE_UNAVAILABLE, TARIF_INDISPONIBLE);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail erreurInattendue(Exception exception) {
        log.error("Erreur inattendue pendant le traitement de la demande", exception);
        return Problemes.de(HttpStatus.INTERNAL_SERVER_ERROR, ERREUR_INTERNE);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErreurChampResponse> erreurs = exception.getBindingResult().getFieldErrors().stream()
                .map(erreur -> new ErreurChampResponse(erreur.getField(), erreur.getDefaultMessage()))
                .sorted(Comparator.comparing(ErreurChampResponse::champ))
                .toList();
        journaliserDemandeRefusee(erreurs.stream().map(ErreurChampResponse::champ).toList());
        return ResponseEntity.badRequest().headers(headers).body(demandeInvalide(erreurs));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (exception.getCause() instanceof JacksonException erreurJson && !erreurJson.getPath().isEmpty()) {
            String champ = cheminDuChamp(erreurJson);
            journaliserDemandeRefusee(List.of(champ));
            ErreurChampResponse erreur = new ErreurChampResponse(champ, messageDuChamp(erreurJson));
            return ResponseEntity.badRequest().headers(headers).body(demandeInvalide(List.of(erreur)));
        }
        log.warn("Demande refusée : corps de requête illisible");
        return ResponseEntity.badRequest()
                .headers(headers)
                .body(Problemes.de(HttpStatus.BAD_REQUEST, JSON_ILLISIBLE));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("Demande refusée : paramètre d'adresse invalide");
        return ResponseEntity.badRequest()
                .headers(headers)
                .body(Problemes.de(HttpStatus.BAD_REQUEST, PARAMETRE_INVALIDE));
    }

    private ProblemDetail probleme(HttpStatus statut, RuntimeException exception) {
        return Problemes.de(statut, exception.getMessage());
    }

    private ProblemDetail demandeInvalide(List<ErreurChampResponse> erreurs) {
        ProblemDetail probleme = Problemes.de(HttpStatus.BAD_REQUEST, DEMANDE_INVALIDE);
        probleme.setProperty(PROPRIETE_ERREURS, erreurs);
        return probleme;
    }

    private void journaliserDemandeRefusee(List<String> champs) {
        log.warn("Demande refusée, champs invalides : {}", champs);
    }

    private String cheminDuChamp(JacksonException erreur) {
        StringBuilder chemin = new StringBuilder();
        erreur.getPath().forEach(reference -> {
            if (reference.getPropertyName() == null) {
                chemin.append('[').append(reference.getIndex()).append(']');
            } else {
                chemin.append(chemin.isEmpty() ? "" : ".").append(reference.getPropertyName());
            }
        });
        return chemin.toString();
    }

    private String messageDuChamp(JacksonException erreur) {
        if (erreur instanceof InvalidFormatException format && format.getTargetType().isEnum()) {
            return VALEUR_NON_AUTORISEE + Arrays.stream(format.getTargetType().getEnumConstants())
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
        }
        return VALEUR_INVALIDE;
    }
}
