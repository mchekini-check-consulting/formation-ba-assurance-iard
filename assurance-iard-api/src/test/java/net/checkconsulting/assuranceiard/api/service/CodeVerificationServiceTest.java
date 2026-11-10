package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.config.DoubleAuthentificationProperties;
import net.checkconsulting.assuranceiard.api.dto.SessionResponse;
import net.checkconsulting.assuranceiard.api.dto.VerificationResponse;
import net.checkconsulting.assuranceiard.api.exception.CodeIncorrectException;
import net.checkconsulting.assuranceiard.api.exception.NouveauCodeRequisException;
import net.checkconsulting.assuranceiard.api.exception.VerificationInconnueException;
import net.checkconsulting.assuranceiard.api.model.CodeVerification;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.repository.CodeVerificationRepository;
import net.checkconsulting.assuranceiard.api.utils.Jetons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CodeVerificationServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-04T10:00:00Z");
    private static final String JETON = "jeton";
    private static final SessionResponse SESSION = new SessionResponse("session", "marie@exemple.fr");

    @Mock
    private CodeVerificationRepository codeRepository;

    @Mock
    private SessionService sessionService;

    @Mock
    private EnvoiCode envoiCode;

    @Mock
    private PasswordEncoder passwordEncoder;

    private Compte compte;
    private CodeVerification verification;

    @BeforeEach
    void preparer() {
        compte = new Compte();
        compte.setEmail("marie@exemple.fr");
        verification = new CodeVerification();
        verification.setCompte(compte);
        verification.setCodeHash("empreinte-code");
        verification.setDateExpiration(MAINTENANT.plusSeconds(60));
        when(codeRepository.findByJetonHash(Jetons.empreinte(JETON))).thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("654321", "empreinte-code")).thenReturn(true);
        when(sessionService.ouvrir(compte)).thenReturn(SESSION);
    }

    private CodeVerificationService service(boolean codeTest) {
        return new CodeVerificationService(codeRepository, sessionService, envoiCode, passwordEncoder,
                new DoubleAuthentificationProperties(codeTest), Clock.fixed(MAINTENANT, ZoneOffset.UTC));
    }

    @Test
    void demarreUneVerificationDeDixMinutesEtRemplaceLaPrecedente() {
        when(passwordEncoder.encode(anyString())).thenReturn("nouvelle-empreinte");

        VerificationResponse reponse = service(true).demarrer(compte);

        assertThat(reponse.emailMasque()).isEqualTo("m***@exemple.fr");
        verify(codeRepository).deleteByCompte(compte);
        ArgumentCaptor<CodeVerification> enregistre = ArgumentCaptor.forClass(CodeVerification.class);
        verify(codeRepository).save(enregistre.capture());
        assertThat(enregistre.getValue().getJetonHash()).isEqualTo(Jetons.empreinte(reponse.verification()));
        assertThat(enregistre.getValue().getCodeHash()).isEqualTo("nouvelle-empreinte");
        assertThat(enregistre.getValue().getDateExpiration()).isEqualTo(MAINTENANT.plus(Duration.ofMinutes(10)));
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(envoiCode).envoyer(eq("marie@exemple.fr"), code.capture());
        assertThat(code.getValue()).matches("\\d{6}");
    }

    @Test
    void ouvreLaSessionAvecLeBonCodeEtConsommeLaVerification() {
        assertThat(service(false).verifier(JETON, "654321")).isEqualTo(SESSION);
        verify(codeRepository).delete(verification);
    }

    @Test
    void accepteLeCodeDeTestQuandIlEstActif() {
        assertThat(service(true).verifier(JETON, "123456")).isEqualTo(SESSION);
    }

    @Test
    void refuseLeCodeDeTestQuandIlEstInactif() {
        assertThatThrownBy(() -> service(false).verifier(JETON, "123456")).isInstanceOf(CodeIncorrectException.class);
    }

    @Test
    void indiqueLesEssaisRestantsApresUnCodeErrone() {
        verification.setErreurs(1);

        assertThatThrownBy(() -> service(true).verifier(JETON, "000000"))
                .isInstanceOfSatisfying(CodeIncorrectException.class, e -> assertThat(e.getEssaisRestants()).isEqualTo(1))
                .hasMessage("Code incorrect.");
        assertThat(verification.getErreurs()).isEqualTo(2);
    }

    @Test
    void imposeUnNouveauCodeALaTroisiemeErreur() {
        verification.setErreurs(2);

        assertThatThrownBy(() -> service(true).verifier(JETON, "000000"))
                .isInstanceOfSatisfying(NouveauCodeRequisException.class,
                        e -> assertThat(e.getMotif()).isEqualTo("essais-epuises"));
        assertThat(verification.getErreurs()).isEqualTo(3);
    }

    @Test
    void refuseMemeLeCodeDeTestApresTroisErreurs() {
        verification.setErreurs(3);

        assertThatThrownBy(() -> service(true).verifier(JETON, "123456")).isInstanceOf(NouveauCodeRequisException.class);
        verify(sessionService, never()).ouvrir(any());
    }

    @Test
    void refuseMemeLeCodeDeTestApresDixMinutes() {
        verification.setDateExpiration(MAINTENANT);

        assertThatThrownBy(() -> service(true).verifier(JETON, "123456"))
                .isInstanceOfSatisfying(NouveauCodeRequisException.class,
                        e -> assertThat(e.getMotif()).isEqualTo("code-expire"));
        verify(sessionService, never()).ouvrir(any());
    }

    @Test
    void leRenvoiDonneUnNouveauCodeTroisEssaisEtDixMinutes() {
        verification.setErreurs(3);
        verification.setDateExpiration(MAINTENANT.minusSeconds(1));
        when(passwordEncoder.encode(anyString())).thenReturn("nouvelle-empreinte");

        VerificationResponse reponse = service(true).renvoyer(JETON);

        assertThat(reponse).isEqualTo(new VerificationResponse(JETON, "m***@exemple.fr"));
        assertThat(verification.getErreurs()).isZero();
        assertThat(verification.getCodeHash()).isEqualTo("nouvelle-empreinte");
        assertThat(verification.getDateExpiration()).isEqualTo(MAINTENANT.plus(Duration.ofMinutes(10)));
        verify(envoiCode).envoyer(eq("marie@exemple.fr"), anyString());
    }

    @Test
    void leCodeEnvoyeParEmailEstCeluiDontSeuleLEmpreinteEstStockee() {
        when(passwordEncoder.encode(anyString())).thenAnswer(appel -> "empreinte-de-" + appel.getArgument(0));

        service(false).demarrer(compte);

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(envoiCode).envoyer(eq("marie@exemple.fr"), code.capture());
        ArgumentCaptor<CodeVerification> enregistre = ArgumentCaptor.forClass(CodeVerification.class);
        verify(codeRepository).save(enregistre.capture());
        assertThat(enregistre.getValue().getCodeHash()).isEqualTo("empreinte-de-" + code.getValue());
    }

    @Test
    void leRenvoiRemplaceLEmpreinteParCelleDuNouveauCodeEnvoye() {
        when(passwordEncoder.encode(anyString())).thenAnswer(appel -> "empreinte-de-" + appel.getArgument(0));

        service(false).renvoyer(JETON);

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(envoiCode).envoyer(eq("marie@exemple.fr"), code.capture());
        assertThat(verification.getCodeHash()).isEqualTo("empreinte-de-" + code.getValue());
    }

    @Test
    void refuseUnJetonInconnu() {
        CodeVerificationService service = service(true);

        assertThatThrownBy(() -> service.verifier("autre", "123456")).isInstanceOf(VerificationInconnueException.class);
        assertThatThrownBy(() -> service.renvoyer("autre")).isInstanceOf(VerificationInconnueException.class);
    }
}
