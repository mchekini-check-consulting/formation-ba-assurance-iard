package net.checkconsulting.assuranceiard.api.service;

import java.time.Clock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.exception.CodeSignatureEpuiseException;
import net.checkconsulting.assuranceiard.api.exception.CodeSignatureExpireException;
import net.checkconsulting.assuranceiard.api.exception.CodeSignatureIncorrectException;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.CodeSignatureContrat;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.repository.CodeSignatureContratRepository;
import net.checkconsulting.assuranceiard.api.utils.Jetons;
import net.checkconsulting.assuranceiard.api.utils.MasqueEmail;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-16 : envoie, renvoi et vérification du code de signature du contrat par e-mail. */
@Service
@RequiredArgsConstructor
public class CodeSignatureContratService {

    private static final int ESSAIS_MAX = 3;
    private static final Duration VALIDITE_CODE = Duration.ofMinutes(10);

    private final CodeSignatureContratRepository codeRepository;
    private final EnvoiEmailContrat envoiEmail;
    private final PasswordEncoder passwordEncoder;
    private final Clock horloge;

    @Transactional
    public String envoyer(Contrat contrat) {
        return envoyerCodePour(contrat);
    }

    @Transactional
    public String renvoyer(Contrat contrat) {
        return envoyerCodePour(contrat);
    }

    @LogMetier
    @Transactional(noRollbackFor = {CodeSignatureIncorrectException.class, CodeSignatureEpuiseException.class})
    public void verifier(Contrat contrat, String code) {
        CodeSignatureContrat csc = codeRepository.findByContrat(contrat)
                .orElseThrow(CodeSignatureEpuiseException::new);
        if (csc.getEssais() >= ESSAIS_MAX) {
            throw new CodeSignatureEpuiseException();
        }
        if (!horloge.instant().isBefore(csc.getDateExpiration())) {
            throw new CodeSignatureExpireException();
        }
        if (!codeValide(code, csc)) {
            csc.setEssais(csc.getEssais() + 1);
            if (csc.getEssais() >= ESSAIS_MAX) {
                throw new CodeSignatureEpuiseException();
            }
            throw new CodeSignatureIncorrectException(ESSAIS_MAX - csc.getEssais());
        }
        codeRepository.deleteByContrat(contrat);
    }

    private String envoyerCodePour(Contrat contrat) {
        String email = contrat.getDossier().getCompte().getEmail();
        codeRepository.deleteByContrat(contrat);
        String code = Jetons.nouveauCode();
        CodeSignatureContrat csc = new CodeSignatureContrat();
        csc.setContrat(contrat);
        csc.setCodeHash(passwordEncoder.encode(code));
        csc.setDateExpiration(horloge.instant().plus(VALIDITE_CODE));
        csc.setEssais(0);
        codeRepository.save(csc);
        envoiEmail.envoyer(email, code);
        return MasqueEmail.masquer(email);
    }

    private boolean codeValide(String code, CodeSignatureContrat csc) {
        return passwordEncoder.matches(code, csc.getCodeHash());
    }
}
