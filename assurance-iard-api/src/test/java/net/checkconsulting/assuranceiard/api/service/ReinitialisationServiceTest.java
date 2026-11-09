package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.config.ConnexionProperties;
import net.checkconsulting.assuranceiard.api.exception.LienInvalideException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.LienReinitialisation;
import net.checkconsulting.assuranceiard.api.repository.CodeVerificationRepository;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.repository.LienReinitialisationRepository;
import net.checkconsulting.assuranceiard.api.utils.Jetons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ReinitialisationServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-04T10:00:00Z");
    private static final String JETON = "jeton";

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private LienReinitialisationRepository lienRepository;

    @Mock
    private CodeVerificationRepository codeRepository;

    @Mock
    private SessionService sessionService;

    @Mock
    private EnvoiLienReinitialisation envoiLien;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ReinitialisationService service;
    private Compte compte;
    private LienReinitialisation lien;

    @BeforeEach
    void preparer() {
        service = new ReinitialisationService(compteRepository, lienRepository, codeRepository, sessionService,
                envoiLien, passwordEncoder, new ConnexionProperties(true, "http://ecran"),
                Clock.fixed(MAINTENANT, ZoneOffset.UTC));
        compte = new Compte();
        compte.setEmail("marie@exemple.fr");
        lien = new LienReinitialisation();
        lien.setCompte(compte);
        lien.setDateExpiration(MAINTENANT.plusSeconds(1));
    }

    private void lienTrouve() {
        when(lienRepository.findByJetonHash(Jetons.empreinte(JETON))).thenReturn(Optional.of(lien));
    }

    @Test
    void envoieUnLienDeVingtQuatreHeuresEtAnnuleLePrecedent() {
        when(compteRepository.findByEmail("marie@exemple.fr")).thenReturn(Optional.of(compte));

        service.demander(" Marie@Exemple.fr ");

        verify(lienRepository).deleteByCompte(compte);
        ArgumentCaptor<LienReinitialisation> enregistre = ArgumentCaptor.forClass(LienReinitialisation.class);
        verify(lienRepository).save(enregistre.capture());
        assertThat(enregistre.getValue().getDateExpiration()).isEqualTo(MAINTENANT.plus(Duration.ofHours(24)));
        ArgumentCaptor<String> envoye = ArgumentCaptor.forClass(String.class);
        verify(envoiLien).envoyer(eq("marie@exemple.fr"), envoye.capture());
        assertThat(envoye.getValue()).startsWith("http://ecran/reinitialisation-mot-de-passe?jeton=");
        String jeton = envoye.getValue().substring(envoye.getValue().indexOf('=') + 1);
        assertThat(enregistre.getValue().getJetonHash()).isEqualTo(Jetons.empreinte(jeton)).isNotEqualTo(jeton);
    }

    @Test
    void neFaitRienSansErreurSiLeCompteNExistePas() {
        when(compteRepository.findByEmail("inconnu@exemple.fr")).thenReturn(Optional.empty());

        service.demander("inconnu@exemple.fr");

        verifyNoInteractions(envoiLien, lienRepository);
    }

    @Test
    void accepteUnLienEncoreValable() {
        lienTrouve();

        service.verifier(JETON);

        verify(lienRepository, never()).delete(any());
    }

    @Test
    void refuseUnLienExpire() {
        lien.setDateExpiration(MAINTENANT);
        lienTrouve();

        assertThatThrownBy(() -> service.verifier(JETON)).isInstanceOf(LienInvalideException.class);
        assertThatThrownBy(() -> service.reinitialiser(JETON, "Nouveau123")).isInstanceOf(LienInvalideException.class);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void refuseUnLienInconnuOuDejaUtilise() {
        when(lienRepository.findByJetonHash(Jetons.empreinte(JETON))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifier(JETON)).isInstanceOf(LienInvalideException.class);
    }

    @Test
    void changeLeMotDePasseConsommeLeLienLeveLeBlocageEtCoupeLesAcces() {
        lienTrouve();
        compte.setEchecsConnexion(5);
        compte.setBloqueJusquA(MAINTENANT.plusSeconds(60));
        when(passwordEncoder.encode("Nouveau123")).thenReturn("nouvelle-empreinte");

        service.reinitialiser(JETON, "Nouveau123");

        assertThat(compte.getMotDePasseHash()).isEqualTo("nouvelle-empreinte");
        assertThat(compte.getEchecsConnexion()).isZero();
        assertThat(compte.getBloqueJusquA()).isNull();
        verify(codeRepository).deleteByCompte(compte);
        verify(sessionService).fermerToutes(compte);
        verify(lienRepository).delete(lien);
    }
}
