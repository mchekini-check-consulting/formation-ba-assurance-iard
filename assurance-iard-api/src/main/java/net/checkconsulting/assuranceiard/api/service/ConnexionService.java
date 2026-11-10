package net.checkconsulting.assuranceiard.api.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.ConnexionRequest;
import net.checkconsulting.assuranceiard.api.dto.VerificationResponse;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.exception.CompteBloqueException;
import net.checkconsulting.assuranceiard.api.exception.IdentifiantsIncorrectsException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Première étape de la connexion : e-mail et mot de passe (AC4, AC5, RG3). */
@Service
public class ConnexionService {

    private static final int ECHECS_MAX = 5;
    private static final Duration DUREE_BLOCAGE = Duration.ofMinutes(15);

    private final CompteRepository compteRepository;
    private final PasswordEncoder passwordEncoder;
    private final CodeVerificationService codeVerificationService;
    private final Clock horloge;
    /** Comparé quand l'e-mail est inconnu : la réponse prend le même temps qu'avec un compte existant. */
    private final String empreinteLeurre;

    public ConnexionService(
            CompteRepository compteRepository,
            PasswordEncoder passwordEncoder,
            CodeVerificationService codeVerificationService,
            Clock horloge) {
        this.compteRepository = compteRepository;
        this.passwordEncoder = passwordEncoder;
        this.codeVerificationService = codeVerificationService;
        this.horloge = horloge;
        this.empreinteLeurre = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /** Le compteur d'échecs et le blocage sont conservés malgré l'exception. */
    @LogMetier
    @Transactional(noRollbackFor = {IdentifiantsIncorrectsException.class, CompteBloqueException.class})
    public VerificationResponse connecter(ConnexionRequest demande) {
        String email = demande.email().trim().toLowerCase(Locale.ROOT);
        Compte compte = compteRepository.findByEmail(email).orElse(null);
        if (compte == null) {
            passwordEncoder.matches(demande.motDePasse(), empreinteLeurre);
            throw new IdentifiantsIncorrectsException();
        }
        Instant maintenant = horloge.instant();
        leverLeBlocageTermine(compte, maintenant);
        if (!passwordEncoder.matches(demande.motDePasse(), compte.getMotDePasseHash())) {
            enregistrerEchec(compte, maintenant);
        }
        compte.setEchecsConnexion(0);
        return codeVerificationService.demarrer(compte);
    }

    private void leverLeBlocageTermine(Compte compte, Instant maintenant) {
        Instant fin = compte.getBloqueJusquA();
        if (fin == null) {
            return;
        }
        if (maintenant.isBefore(fin)) {
            throw new CompteBloqueException(fin);
        }
        compte.setBloqueJusquA(null);
        compte.setEchecsConnexion(0);
    }

    private void enregistrerEchec(Compte compte, Instant maintenant) {
        compte.setEchecsConnexion(compte.getEchecsConnexion() + 1);
        if (compte.getEchecsConnexion() >= ECHECS_MAX) {
            Instant fin = maintenant.plus(DUREE_BLOCAGE);
            compte.setBloqueJusquA(fin);
            throw new CompteBloqueException(fin);
        }
        throw new IdentifiantsIncorrectsException();
    }
}
