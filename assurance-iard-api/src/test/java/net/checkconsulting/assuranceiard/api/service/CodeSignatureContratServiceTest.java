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
import net.checkconsulting.assuranceiard.api.exception.CodeSignatureEpuiseException;
import net.checkconsulting.assuranceiard.api.exception.CodeSignatureExpireException;
import net.checkconsulting.assuranceiard.api.exception.CodeSignatureIncorrectException;
import net.checkconsulting.assuranceiard.api.model.CodeSignatureContrat;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.repository.CodeSignatureContratRepository;
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
class CodeSignatureContratServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-05T10:00:00Z");

    @Mock
    private CodeSignatureContratRepository codeRepository;

    @Mock
    private EnvoiEmailContrat envoiEmail;

    @Mock
    private PasswordEncoder passwordEncoder;

    private Contrat contrat;
    private CodeSignatureContrat codeSignature;

    @BeforeEach
    void preparer() {
        Compte compte = new Compte();
        compte.setEmail("jean.dupont@example.com");
        DossierSouscription dossier = new DossierSouscription();
        dossier.setCompte(compte);
        contrat = new Contrat();
        contrat.setDossier(dossier);

        codeSignature = new CodeSignatureContrat();
        codeSignature.setContrat(contrat);
        codeSignature.setCodeHash("empreinte-code");
        codeSignature.setDateExpiration(MAINTENANT.plusSeconds(60));
        codeSignature.setEssais(0);

        when(codeRepository.findByContrat(contrat)).thenReturn(Optional.of(codeSignature));
        when(passwordEncoder.matches("654321", "empreinte-code")).thenReturn(true);
    }

    private CodeSignatureContratService service() {
        return new CodeSignatureContratService(codeRepository, envoiEmail, passwordEncoder,
                Clock.fixed(MAINTENANT, ZoneOffset.UTC));
    }

    @Test
    void envoyerSupprimeAncienCodeSauvegardeLeNouveauEtRetourneLEmailMasque() {
        when(passwordEncoder.encode(anyString())).thenReturn("nouvelle-empreinte");

        String emailMasque = service().envoyer(contrat);

        assertThat(emailMasque).isEqualTo("j***@example.com");
        verify(codeRepository).deleteByContrat(contrat);
        ArgumentCaptor<CodeSignatureContrat> enregistre = ArgumentCaptor.forClass(CodeSignatureContrat.class);
        verify(codeRepository).save(enregistre.capture());
        assertThat(enregistre.getValue().getCodeHash()).isEqualTo("nouvelle-empreinte");
        assertThat(enregistre.getValue().getEssais()).isZero();
        assertThat(enregistre.getValue().getDateExpiration()).isEqualTo(MAINTENANT.plus(Duration.ofMinutes(10)));
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(envoiEmail).envoyer(eq("jean.dupont@example.com"), code.capture());
        assertThat(code.getValue()).matches("\\d{6}");
    }

    @Test
    void renvoyerSeComporteCommeEnvoyer() {
        when(passwordEncoder.encode(anyString())).thenReturn("nouvelle-empreinte");

        String emailMasque = service().renvoyer(contrat);

        assertThat(emailMasque).isEqualTo("j***@example.com");
        verify(codeRepository).deleteByContrat(contrat);
        verify(codeRepository).save(any(CodeSignatureContrat.class));
        verify(envoiEmail).envoyer(eq("jean.dupont@example.com"), anyString());
    }

    @Test
    void verifierAccepteLeCodeCorrectEtSupprimeEnregistrement() {
        service().verifier(contrat, "654321");

        verify(codeRepository).deleteByContrat(contrat);
    }

    @Test
    void verifierIndiqueEssaisRestantsApresUnCodeErrone() {
        codeSignature.setEssais(1);

        assertThatThrownBy(() -> service().verifier(contrat, "000000"))
                .isInstanceOfSatisfying(CodeSignatureIncorrectException.class,
                        e -> assertThat(e.getEssaisRestants()).isEqualTo(1))
                .hasMessage("Code incorrect. Il vous reste 1 essai(s).");
        assertThat(codeSignature.getEssais()).isEqualTo(2);
    }

    @Test
    void verifierLanceEpuiseLorsQueLeTroisièmeEssaiEchoue() {
        codeSignature.setEssais(2);

        assertThatThrownBy(() -> service().verifier(contrat, "000000"))
                .isInstanceOf(CodeSignatureEpuiseException.class);
        assertThat(codeSignature.getEssais()).isEqualTo(3);
    }

    @Test
    void verifierLanceEpuiseSiEssaisDejaMaximum() {
        codeSignature.setEssais(3);

        assertThatThrownBy(() -> service().verifier(contrat, "000000"))
                .isInstanceOf(CodeSignatureEpuiseException.class);
        verify(codeRepository, never()).deleteByContrat(contrat);
    }

    @Test
    void verifierLanceExpireSiCodeDepasse() {
        codeSignature.setDateExpiration(MAINTENANT);

        assertThatThrownBy(() -> service().verifier(contrat, "000000"))
                .isInstanceOf(CodeSignatureExpireException.class);
        verify(codeRepository, never()).deleteByContrat(contrat);
    }

    @Test
    void verifierLanceEpuiseSiAucunEnregistrement() {
        when(codeRepository.findByContrat(contrat)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().verifier(contrat, "000000"))
                .isInstanceOf(CodeSignatureEpuiseException.class);
    }
}
