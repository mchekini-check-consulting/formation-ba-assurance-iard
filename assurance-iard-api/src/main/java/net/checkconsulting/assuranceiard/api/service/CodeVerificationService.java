package net.checkconsulting.assuranceiard.api.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.config.ConnexionProperties;
import net.checkconsulting.assuranceiard.api.dto.SessionResponse;
import net.checkconsulting.assuranceiard.api.dto.VerificationResponse;
import net.checkconsulting.assuranceiard.api.exception.CodeIncorrectException;
import net.checkconsulting.assuranceiard.api.exception.NouveauCodeRequisException;
import net.checkconsulting.assuranceiard.api.exception.VerificationInconnueException;
import net.checkconsulting.assuranceiard.api.model.CodeVerification;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.repository.CodeVerificationRepository;
import net.checkconsulting.assuranceiard.api.utils.Jetons;
import net.checkconsulting.assuranceiard.api.utils.MasqueEmail;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RG1 et RG2 : code à 6 chiffres, valable 10 minutes, 3 essais maximum, renvoi possible. */
@Service
@RequiredArgsConstructor
public class CodeVerificationService {

    static final String CODE_BOUCHON = "123456";
    private static final int ESSAIS_MAX = 3;
    private static final Duration VALIDITE_CODE = Duration.ofMinutes(10);

    private final CodeVerificationRepository codeRepository;
    private final SessionService sessionService;
    private final EnvoiCode envoiCode;
    private final PasswordEncoder passwordEncoder;
    private final ConnexionProperties proprietes;
    private final Clock horloge;

    /** Remplace la vérification en cours du compte : un seul code valable à la fois. */
    @Transactional
    public VerificationResponse demarrer(Compte compte) {
        codeRepository.deleteByCompte(compte);
        String jeton = Jetons.nouveauJeton();
        CodeVerification verification = new CodeVerification();
        verification.setCompte(compte);
        verification.setJetonHash(Jetons.empreinte(jeton));
        nouveauCode(verification);
        codeRepository.save(verification);
        return new VerificationResponse(jeton, MasqueEmail.masquer(compte.getEmail()));
    }

    @Transactional
    public VerificationResponse renvoyer(String jeton) {
        CodeVerification verification = trouver(jeton);
        nouveauCode(verification);
        return new VerificationResponse(jeton, MasqueEmail.masquer(verification.getCompte().getEmail()));
    }

    /** Les erreurs de code sont conservées malgré l'exception : elles comptent dans les 3 essais. */
    @Transactional(noRollbackFor = {CodeIncorrectException.class, NouveauCodeRequisException.class})
    public SessionResponse verifier(String jeton, String code) {
        CodeVerification verification = trouver(jeton);
        if (verification.getErreurs() >= ESSAIS_MAX) {
            throw NouveauCodeRequisException.essaisEpuises();
        }
        Instant maintenant = horloge.instant();
        if (!maintenant.isBefore(verification.getDateExpiration())) {
            throw NouveauCodeRequisException.codeExpire();
        }
        if (!codeValide(code, verification)) {
            verification.setErreurs(verification.getErreurs() + 1);
            if (verification.getErreurs() >= ESSAIS_MAX) {
                throw NouveauCodeRequisException.essaisEpuises();
            }
            throw new CodeIncorrectException(ESSAIS_MAX - verification.getErreurs());
        }
        Compte compte = verification.getCompte();
        codeRepository.delete(verification);
        return sessionService.ouvrir(compte);
    }

    private CodeVerification trouver(String jeton) {
        return codeRepository.findByJetonHash(Jetons.empreinte(jeton)).orElseThrow(VerificationInconnueException::new);
    }

    private void nouveauCode(CodeVerification verification) {
        String code = Jetons.nouveauCode();
        verification.setCodeHash(passwordEncoder.encode(code));
        verification.setErreurs(0);
        verification.setDateExpiration(horloge.instant().plus(VALIDITE_CODE));
        envoiCode.envoyer(verification.getCompte().getEmail(), code);
    }

    private boolean codeValide(String code, CodeVerification verification) {
        return proprietes.codeBouchonActif() && CODE_BOUCHON.equals(code)
                || passwordEncoder.matches(code, verification.getCodeHash());
    }
}
