package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.ConnexionRequest;
import net.checkconsulting.assuranceiard.api.dto.VerificationResponse;
import net.checkconsulting.assuranceiard.api.exception.CompteBloqueException;
import net.checkconsulting.assuranceiard.api.exception.IdentifiantsIncorrectsException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConnexionServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-04T10:00:00Z");
    private static final ConnexionRequest BON = new ConnexionRequest(" Marie@Exemple.fr ", "bon");
    private static final ConnexionRequest MAUVAIS = new ConnexionRequest("marie@exemple.fr", "mauvais");

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CodeVerificationService codeVerificationService;

    private ConnexionService connexionService;
    private Compte compte;

    @BeforeEach
    void preparer() {
        when(passwordEncoder.encode(anyString())).thenReturn("leurre");
        when(passwordEncoder.matches("bon", "empreinte")).thenReturn(true);
        compte = new Compte();
        compte.setEmail("marie@exemple.fr");
        compte.setMotDePasseHash("empreinte");
        when(compteRepository.findByEmail("marie@exemple.fr")).thenReturn(Optional.of(compte));
        connexionService = new ConnexionService(
                compteRepository, passwordEncoder, codeVerificationService, Clock.fixed(MAINTENANT, ZoneOffset.UTC));
    }

    @Test
    void lanceLaVerificationEtRemetLeCompteurAZeroSiLeMotDePasseEstBon() {
        compte.setEchecsConnexion(4);
        VerificationResponse attendu = new VerificationResponse("jeton", "m***@exemple.fr");
        when(codeVerificationService.demarrer(compte)).thenReturn(attendu);

        assertThat(connexionService.connecter(BON)).isEqualTo(attendu);
        assertThat(compte.getEchecsConnexion()).isZero();
    }

    @Test
    void repondLaMemeChoseSiLeMotDePasseEstFauxOuLEmailInconnu() {
        when(compteRepository.findByEmail("inconnu@exemple.fr")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connexionService.connecter(MAUVAIS))
                .isInstanceOf(IdentifiantsIncorrectsException.class)
                .hasMessage("E-mail ou mot de passe incorrect.");
        assertThatThrownBy(() -> connexionService.connecter(new ConnexionRequest("inconnu@exemple.fr", "bon")))
                .isInstanceOf(IdentifiantsIncorrectsException.class)
                .hasMessage("E-mail ou mot de passe incorrect.");

        verify(passwordEncoder).matches("bon", "leurre");
        assertThat(compte.getEchecsConnexion()).isEqualTo(1);
        verify(codeVerificationService, never()).demarrer(any());
    }

    @Test
    void bloqueLeCompte15MinutesAuCinquiemeEchecConsecutif() {
        compte.setEchecsConnexion(4);

        assertThatThrownBy(() -> connexionService.connecter(MAUVAIS))
                .isInstanceOf(CompteBloqueException.class)
                .hasMessage("Compte bloqué 15 minutes.");

        assertThat(compte.getEchecsConnexion()).isEqualTo(5);
        assertThat(compte.getBloqueJusquA()).isEqualTo(MAINTENANT.plus(Duration.ofMinutes(15)));
    }

    @Test
    void refuseMemeLeBonMotDePassePendantLeBlocage() {
        Instant fin = MAINTENANT.plusSeconds(60);
        compte.setBloqueJusquA(fin);

        assertThatThrownBy(() -> connexionService.connecter(BON))
                .isInstanceOfSatisfying(CompteBloqueException.class, e -> assertThat(e.getBloqueJusquA()).isEqualTo(fin));
        verify(codeVerificationService, never()).demarrer(any());
    }

    @Test
    void repartDeZeroQuandLeBlocageEstTermine() {
        compte.setEchecsConnexion(5);
        compte.setBloqueJusquA(MAINTENANT);

        assertThatThrownBy(() -> connexionService.connecter(MAUVAIS)).isInstanceOf(IdentifiantsIncorrectsException.class);

        assertThat(compte.getBloqueJusquA()).isNull();
        assertThat(compte.getEchecsConnexion()).isEqualTo(1);
    }
}
