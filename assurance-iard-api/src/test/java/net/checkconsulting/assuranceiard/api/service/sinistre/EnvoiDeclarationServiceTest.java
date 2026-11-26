package net.checkconsulting.assuranceiard.api.service.sinistre;

import static net.checkconsulting.assuranceiard.api.service.sinistre.FixturesSinistre.complet;
import static net.checkconsulting.assuranceiard.api.service.sinistre.FixturesSinistre.contrat;
import static net.checkconsulting.assuranceiard.api.service.sinistre.FixturesSinistre.present;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.EnvoiSinistreRequest;
import net.checkconsulting.assuranceiard.api.dto.SinistreEnvoyeResponse;
import net.checkconsulting.assuranceiard.api.exception.DeclarationDejaEnvoyeeException;
import net.checkconsulting.assuranceiard.api.exception.DeclarationInvalideException;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.Sinistre;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.SinistreRepository;
import net.checkconsulting.assuranceiard.api.service.EnvoiSinistre;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;

/** US-24, AC9, AC13, RG5, RG8, RG9, RG11 : envoi de la déclaration et e-mails. */
class EnvoiDeclarationServiceTest {

    private static final EnvoiSinistreRequest SUR_L_HONNEUR = new EnvoiSinistreRequest(true);

    private final FixturesSinistre fixtures = new FixturesSinistre();
    private final ContratRepository contrats = mock(ContratRepository.class);
    private final SinistreRepository sinistres = mock(SinistreRepository.class);
    private final EnvoiSinistre emails = mock(EnvoiSinistre.class);
    private final DeclarationSinistreService declarations = new DeclarationSinistreService(mock(SessionService.class),
            contrats, sinistres, fixtures.referentiel, fixtures.vue, fixtures.json, fixtures.proprietes);
    private final EnvoiDeclarationService service = new EnvoiDeclarationService(declarations, sinistres,
            fixtures.referentiel, fixtures.vue, fixtures.json, emails);

    private final Contrat a03 = contrat(3L, "AE-RECETTE-A03", "ACTIF", "TR");
    private Sinistre sinistre;

    @BeforeEach
    void preparer() {
        sinistre = complet(42L, 3L, "VOL");
        when(sinistres.findById(42L)).thenReturn(Optional.of(sinistre));
        when(contrats.findById(3L)).thenReturn(present(a03));
        when(sinistres.save(any(Sinistre.class))).thenAnswer(appel -> appel.getArgument(0));
    }

    @Test
    void ac9LeDossierEstCreeAvecSonNumeroAuStatutDeclare() {
        SinistreEnvoyeResponse envoye = service.envoyer(42L, SUR_L_HONNEUR);

        assertThat(envoye.numero()).isEqualTo("SIN-2026-000042").matches("SIN-\\d{4}-\\d{6}");
        assertThat(envoye.statut()).isEqualTo("DECLARE");
        assertThat(envoye.declareLe()).isEqualTo(FixturesSinistre.MAINTENANT);
        assertThat(envoye.emailEnvoye()).isTrue();
        assertThat(sinistre.getDeclarationHonneur()).isTrue();
        assertThat(sinistre.getCorbeille()).isEqualTo("AUTO");
        assertThat(sinistre.getRequalifier()).isFalse();
        verify(sinistres).save(sinistre);
    }

    @Test
    void ac7UnePieceObligatoireManquanteNEmpechePasLEnvoi() {
        SinistreEnvoyeResponse envoye = service.envoyer(42L, SUR_L_HONNEUR);

        assertThat(envoye.piecesManquantes()).isTrue();
        assertThat(sinistre.getPiecesManquantes()).isTrue();
    }

    @Test
    void ac9AccuseDeReceptionSansDonneeDeSante() {
        sinistre.setBlesses(true);
        sinistre.setNombreBlesses((short) 2);

        service.envoyer(42L, SUR_L_HONNEUR);

        ArgumentCaptor<EnvoiSinistre.Dossier> dossier = ArgumentCaptor.forClass(EnvoiSinistre.Dossier.class);
        verify(emails).accuserReception(dossier.capture());
        verify(emails, never()).informerSouscripteur(any());
        assertThat(dossier.getValue().email()).isEqualTo("recette.auto@assureasy.fr");
        assertThat(dossier.getValue().numeroSinistre()).isEqualTo("SIN-2026-000042");
        assertThat(dossier.getValue().libelleType()).isEqualTo("Vol du véhicule");
        // Le dossier transmis aux e-mails ne contient ni blessés ni description des faits.
        assertThat(dossier.getValue().toString()).doesNotContain("bless", "Véhicule disparu");
    }

    @Test
    void ac13UnProcheDeclareLeSouscripteurEstInforme() {
        sinistre.setDeclarant("PROCHE");
        sinistre.setDeclarantInfos(fixtures.json.ecrireDeclarant("Léa Durand", "Fille"));

        service.envoyer(42L, SUR_L_HONNEUR);

        ArgumentCaptor<EnvoiSinistre.Dossier> dossier = ArgumentCaptor.forClass(EnvoiSinistre.Dossier.class);
        verify(emails).informerSouscripteur(dossier.capture());
        assertThat(dossier.getValue().email()).isEqualTo("recette.auto@assureasy.fr");
        assertThat(dossier.getValue().declarantNom()).isEqualTo("Léa Durand");
        assertThat(dossier.getValue().declarantLien()).isEqualTo("Fille");
    }

    @Test
    void rg8SansLaDeclarationSurLHonneurRienNEstEnvoye() {
        assertThatThrownBy(() -> service.envoyer(42L, new EnvoiSinistreRequest(false)))
                .isInstanceOf(DeclarationInvalideException.class).hasMessageContaining("sur l'honneur");
        assertThatThrownBy(() -> service.envoyer(42L, null)).isInstanceOf(DeclarationInvalideException.class);
        verify(sinistres, never()).save(any());
        verifyNoInteractions(emails);
    }

    @Test
    void ac6EnRetardSansExplicationLEnvoiEstRefuseAvecUnMessageClair() {
        sinistre.setDateSurvenance(FixturesSinistre.AUJOURDHUI.minusDays(20));

        assertThatThrownBy(() -> service.envoyer(42L, SUR_L_HONNEUR))
                .isInstanceOf(DeclarationInvalideException.class).hasMessageContaining("raison du délai");

        sinistre.setExplicationRetard("J'étais à l'étranger");
        assertThat(service.envoyer(42L, SUR_L_HONNEUR).statut()).isEqualTo("DECLARE");
        assertThat(sinistre.getHorsDelai()).isTrue();
    }

    @Test
    void rg11AutreEstSignaleARequalifier() {
        sinistre.setType("AUTRE");

        service.envoyer(42L, SUR_L_HONNEUR);

        assertThat(sinistre.getRequalifier()).isTrue();
        assertThat(sinistre.getPiecesManquantes()).isFalse();
    }

    @Test
    void unEMailQuiNePartPasNAnnulePasLaDeclaration() {
        doThrow(new MailSendException("SMTP indisponible")).when(emails).accuserReception(any());

        SinistreEnvoyeResponse envoye = service.envoyer(42L, SUR_L_HONNEUR);

        assertThat(envoye.statut()).isEqualTo("DECLARE");
        assertThat(envoye.emailEnvoye()).isFalse();
    }

    @Test
    void uneDeclarationNePartQuUneFois() {
        sinistre.setStatut("DECLARE");

        assertThatThrownBy(() -> service.envoyer(42L, SUR_L_HONNEUR))
                .isInstanceOf(DeclarationDejaEnvoyeeException.class);
    }
}
