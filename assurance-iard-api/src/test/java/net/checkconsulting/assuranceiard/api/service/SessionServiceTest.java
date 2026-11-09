package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.SessionCouranteResponse;
import net.checkconsulting.assuranceiard.api.dto.SessionResponse;
import net.checkconsulting.assuranceiard.api.exception.SessionInvalideException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.SessionClient;
import net.checkconsulting.assuranceiard.api.repository.SessionClientRepository;
import net.checkconsulting.assuranceiard.api.utils.Jetons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-04T10:00:00Z");

    @Mock
    private SessionClientRepository sessionRepository;

    private SessionService sessionService;
    private Compte compte;

    @BeforeEach
    void preparer() {
        sessionService = new SessionService(sessionRepository, Clock.fixed(MAINTENANT, ZoneOffset.UTC));
        compte = new Compte();
        compte.setEmail("marie@exemple.fr");
    }

    private SessionClient sessionExpirantA(Instant date) {
        SessionClient session = new SessionClient();
        session.setCompte(compte);
        session.setDateExpiration(date);
        return session;
    }

    @Test
    void ouvreUneSessionDeTrenteMinutesSansStockerLeJetonEnClair() {
        SessionResponse reponse = sessionService.ouvrir(compte);

        assertThat(reponse.email()).isEqualTo("marie@exemple.fr");
        verify(sessionRepository).deleteByCompteAndDateExpirationBefore(compte, MAINTENANT);
        ArgumentCaptor<SessionClient> enregistre = ArgumentCaptor.forClass(SessionClient.class);
        verify(sessionRepository).save(enregistre.capture());
        assertThat(enregistre.getValue().getJetonHash()).isEqualTo(Jetons.empreinte(reponse.jeton()));
        assertThat(enregistre.getValue().getJetonHash()).isNotEqualTo(reponse.jeton());
        assertThat(enregistre.getValue().getDateExpiration()).isEqualTo(MAINTENANT.plus(Duration.ofMinutes(30)));
    }

    @Test
    void litLaSessionEnCoursEtLaProlongeDeTrenteMinutes() {
        SessionClient session = sessionExpirantA(MAINTENANT.plusSeconds(1));
        when(sessionRepository.findByJetonHash(Jetons.empreinte("jeton"))).thenReturn(Optional.of(session));

        assertThat(sessionService.lire("Bearer jeton")).isEqualTo(new SessionCouranteResponse("marie@exemple.fr"));
        assertThat(session.getDateExpiration()).isEqualTo(MAINTENANT.plus(Duration.ofMinutes(30)));
    }

    @Test
    void neProlongePasUneSessionExpiree() {
        SessionClient session = sessionExpirantA(MAINTENANT.minusSeconds(1));
        when(sessionRepository.findByJetonHash(Jetons.empreinte("jeton"))).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.lire("Bearer jeton")).isInstanceOf(SessionInvalideException.class);
        assertThat(session.getDateExpiration()).isEqualTo(MAINTENANT.minusSeconds(1));
    }

    @Test
    void fermeToutesLesSessionsDuCompte() {
        sessionService.fermerToutes(compte);

        verify(sessionRepository).deleteByCompte(compte);
    }

    @Test
    void refuseUneSessionExpiree() {
        when(sessionRepository.findByJetonHash(Jetons.empreinte("jeton")))
                .thenReturn(Optional.of(sessionExpirantA(MAINTENANT)));

        assertThatThrownBy(() -> sessionService.lire("Bearer jeton")).isInstanceOf(SessionInvalideException.class);
    }

    @Test
    void refuseUnJetonInconnuOuAbsent() {
        when(sessionRepository.findByJetonHash(Jetons.empreinte("inconnu"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.lire("Bearer inconnu")).isInstanceOf(SessionInvalideException.class);
        assertThatThrownBy(() -> sessionService.lire(null)).isInstanceOf(SessionInvalideException.class);
        assertThatThrownBy(() -> sessionService.lire("Basic abc")).isInstanceOf(SessionInvalideException.class);
    }

    @Test
    void fermeLaSession() {
        sessionService.fermer("Bearer jeton");

        verify(sessionRepository).deleteByJetonHash(Jetons.empreinte("jeton"));
    }
}
