package net.checkconsulting.assuranceiard.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GestionErreurs {

    @ExceptionHandler(EmailDejaUtiliseException.class)
    public ProblemDetail emailDejaUtilise(EmailDejaUtiliseException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(IdentifiantsIncorrectsException.class)
    public ProblemDetail identifiantsIncorrects(IdentifiantsIncorrectsException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(CompteBloqueException.class)
    public ProblemDetail compteBloque(CompteBloqueException exception) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.LOCKED, exception.getMessage());
        probleme.setProperty("bloqueJusquA", exception.getBloqueJusquA().toString());
        return probleme;
    }

    @ExceptionHandler(CodeIncorrectException.class)
    public ProblemDetail codeIncorrect(CodeIncorrectException exception) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        probleme.setProperty("essaisRestants", exception.getEssaisRestants());
        return probleme;
    }

    @ExceptionHandler(NouveauCodeRequisException.class)
    public ProblemDetail nouveauCodeRequis(NouveauCodeRequisException exception) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        probleme.setProperty("motif", exception.getMotif());
        return probleme;
    }

    @ExceptionHandler(VerificationInconnueException.class)
    public ProblemDetail verificationInconnue(VerificationInconnueException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(LienInvalideException.class)
    public ProblemDetail lienInvalide(LienInvalideException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.GONE, exception.getMessage());
    }

    @ExceptionHandler(SessionInvalideException.class)
    public ProblemDetail sessionInvalide(SessionInvalideException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(InformationRefuseeException.class)
    public ProblemDetail informationRefusee(InformationRefuseeException exception) {
        ProblemDetail probleme =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
        probleme.setProperty("champ", exception.getChamp());
        return probleme;
    }

    @ExceptionHandler(DevisInconnuException.class)
    public ProblemDetail devisInconnu(DevisInconnuException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(MandatNonSigneException.class)
    public ProblemDetail mandatNonSigne(MandatNonSigneException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(CodeSignatureIncorrectException.class)
    public ProblemDetail codeSignatureIncorrect(CodeSignatureIncorrectException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail fichierTropGros(MaxUploadSizeExceededException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, "Fichier trop volumineux.");
    }
}
