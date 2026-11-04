package net.checkconsulting.assuranceiard.api.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.SessionCouranteResponse;
import net.checkconsulting.assuranceiard.api.dto.SessionResponse;
import net.checkconsulting.assuranceiard.api.exception.SessionInvalideException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.SessionClient;
import net.checkconsulting.assuranceiard.api.repository.SessionClientRepository;
import net.checkconsulting.assuranceiard.api.utils.Jetons;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final Duration DUREE_SESSION = Duration.ofMinutes(30);
    private static final String PREFIXE_BEARER = "Bearer ";

    private final SessionClientRepository sessionRepository;
    private final Clock horloge;

    @Transactional
    public SessionResponse ouvrir(Compte compte) {
        Instant maintenant = horloge.instant();
        sessionRepository.deleteByCompteAndDateExpirationBefore(compte, maintenant);
        String jeton = Jetons.nouveauJeton();
        SessionClient session = new SessionClient();
        session.setCompte(compte);
        session.setJetonHash(Jetons.empreinte(jeton));
        session.setDateExpiration(maintenant.plus(DUREE_SESSION));
        sessionRepository.save(session);
        return new SessionResponse(jeton, compte.getEmail());
    }

    @Transactional(readOnly = true)
    public SessionCouranteResponse lire(String authorization) {
        return sessionRepository.findByJetonHash(empreinte(authorization))
                .filter(session -> horloge.instant().isBefore(session.getDateExpiration()))
                .map(session -> new SessionCouranteResponse(session.getCompte().getEmail()))
                .orElseThrow(SessionInvalideException::new);
    }

    @Transactional
    public void fermer(String authorization) {
        sessionRepository.deleteByJetonHash(empreinte(authorization));
    }

    private String empreinte(String authorization) {
        if (authorization == null || !authorization.startsWith(PREFIXE_BEARER)) {
            throw new SessionInvalideException();
        }
        return Jetons.empreinte(authorization.substring(PREFIXE_BEARER.length()));
    }
}
