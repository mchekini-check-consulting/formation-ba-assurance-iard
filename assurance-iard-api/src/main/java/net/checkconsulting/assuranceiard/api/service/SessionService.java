package net.checkconsulting.assuranceiard.api.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.SessionCouranteResponse;
import net.checkconsulting.assuranceiard.api.dto.SessionResponse;
import net.checkconsulting.assuranceiard.api.exception.SessionInvalideException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Permission;
import net.checkconsulting.assuranceiard.api.model.SessionClient;
import net.checkconsulting.assuranceiard.api.repository.PermissionRepository;
import net.checkconsulting.assuranceiard.api.repository.SessionClientRepository;
import net.checkconsulting.assuranceiard.api.utils.Jetons;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RG3 de l'US-07 : la session prend fin après 30 minutes sans activité. */
@Service
@RequiredArgsConstructor
public class SessionService {

    private static final Duration DUREE_INACTIVITE = Duration.ofMinutes(30);
    private static final String PREFIXE_BEARER = "Bearer ";

    private final SessionClientRepository sessionRepository;
    private final PermissionRepository permissionRepository;
    private final Clock horloge;

    @Transactional
    public SessionResponse ouvrir(Compte compte) {
        Instant maintenant = horloge.instant();
        sessionRepository.deleteByCompteAndDateExpirationBefore(compte, maintenant);
        String jeton = Jetons.nouveauJeton();
        SessionClient session = new SessionClient();
        session.setCompte(compte);
        session.setJetonHash(Jetons.empreinte(jeton));
        session.setDateExpiration(maintenant.plus(DUREE_INACTIVITE));
        sessionRepository.save(session);
        return new SessionResponse(jeton, compte.getEmail(), compte.getProfil().name());
    }

    /**
     * Toute lecture d'une session valable compte comme une activité et la prolonge de 30 minutes.
     * US-04 : renvoie aussi les droits du profil, relus en base, pour que les écrans suivent un droit retiré (AC5).
     */
    @Transactional
    public SessionCouranteResponse lire(String authorization) {
        Compte compte = compteConnecte(authorization);
        List<String> droits = permissionRepository.findByProfil(compte.getProfil()).stream()
                .filter(Permission::isValeur)
                .map(permission -> permission.getRubrique().name() + ":" + permission.getDroit().name())
                .sorted()
                .toList();
        return new SessionCouranteResponse(compte.getEmail(), compte.getProfil().name(), droits);
    }

    /** Renvoie le compte de la session, en la prolongeant ; refuse une session absente ou expirée. */
    @Transactional
    public Compte compteConnecte(String authorization) {
        Instant maintenant = horloge.instant();
        SessionClient session = sessionRepository.findByJetonHash(empreinte(authorization))
                .filter(trouvee -> maintenant.isBefore(trouvee.getDateExpiration()))
                .orElseThrow(SessionInvalideException::new);
        session.setDateExpiration(maintenant.plus(DUREE_INACTIVITE));
        Compte compte = session.getCompte();
        compte.getEmail(); // force l'initialisation du proxy JPA avant la fin de la transaction
        return compte;
    }

    @Transactional
    public void fermer(String authorization) {
        sessionRepository.deleteByJetonHash(empreinte(authorization));
    }

    @Transactional
    public void fermerToutes(Compte compte) {
        sessionRepository.deleteByCompte(compte);
    }

    private String empreinte(String authorization) {
        if (authorization == null || !authorization.startsWith(PREFIXE_BEARER)) {
            throw new SessionInvalideException();
        }
        return Jetons.empreinte(authorization.substring(PREFIXE_BEARER.length()));
    }
}
