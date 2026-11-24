package net.checkconsulting.assuranceiard.api.service.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.RegenerationResponse;
import net.checkconsulting.assuranceiard.api.exception.ContratInconnuException;
import net.checkconsulting.assuranceiard.api.exception.RegenerationImpossibleException;
import net.checkconsulting.assuranceiard.api.exception.RegenerationTropRapprocheeException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.service.DocumentService;
import net.checkconsulting.assuranceiard.api.service.EnvoiConditionsParticulieres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailSendException;
import org.springframework.transaction.support.TransactionOperations;

class RegenerationContratServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-24T10:00:00Z");
    private static final String NUMERO = "AE-RECETTE-A03";
    private static final byte[] PDF = {'%', 'P', 'D', 'F'};

    private final ContratRepository contrats = mock(ContratRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final DocumentsGeneresService generation = mock(DocumentsGeneresService.class);
    private final EnvoiConditionsParticulieres envoi = mock(EnvoiConditionsParticulieres.class);
    private final RegenerationContratService service = new RegenerationContratService(contrats, documents, generation,
            envoi, TransactionOperations.withoutTransaction(), Clock.fixed(MAINTENANT, ZoneOffset.UTC));

    private Contrat contrat;

    @BeforeEach
    void preparer() {
        Compte compte = new Compte();
        compte.setEmail("camille@exemple.fr");
        compte.setPrenom("Camille");
        contrat = new Contrat();
        contrat.setId(3L);
        contrat.setNumero(NUMERO);
        contrat.setStatut("ACTIF");
        contrat.setCompte(compte);
        when(contrats.parNumero(NUMERO)).thenReturn(Optional.of(contrat));
        when(documents.enVigueurVerrouille(3L, "CONDITIONS_PARTICULIERES", "")).thenReturn(Optional.empty());
    }

    private static Document version(int numero, boolean stocke, Instant creation) {
        Document document = new Document();
        document.setId(100L + numero);
        document.setVersion(numero);
        document.setStocke(stocke);
        document.setDateCreation(creation);
        return document;
    }

    @Test
    void ac1EtAc3RegenereLaVersionSuivanteEtLEnvoieParEmail() {
        Document v2 = version(2, true, MAINTENANT);
        when(generation.generer(any())).thenReturn(v2);
        when(generation.telecharger(102L)).thenReturn(new DocumentService.Fichier("cp-v2.pdf", "pdf", PDF));

        RegenerationResponse reponse = service.regenerer(NUMERO);

        assertThat(reponse).isEqualTo(new RegenerationResponse(102L, 2, MAINTENANT, true, true));
        ArgumentCaptor<DemandeDocument> demande = ArgumentCaptor.forClass(DemandeDocument.class);
        verify(generation).generer(demande.capture());
        assertThat(demande.getValue()).isEqualTo(DemandeDocument.duContrat(TypeDocument.CONDITIONS_PARTICULIERES, 3L,
                RegenerationContratService.MOTIF));
        verify(envoi).envoyer("camille@exemple.fr", "Camille", NUMERO, 2, "cp-v2.pdf", PDF);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIF_JUSTIFICATIFS_ATTENDUS", "AVENANT_EN_ATTENTE"})
    void lesAutresStatutsPermisParRg3(String statut) {
        contrat.setStatut(statut);
        when(generation.generer(any())).thenReturn(version(2, true, MAINTENANT));
        when(generation.telecharger(102L)).thenReturn(new DocumentService.Fichier("cp.pdf", "pdf", PDF));

        assertThat(service.regenerer(NUMERO).version()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUSPENDU", "EN_ATTENTE_DE_SIGNATURE", "EN_ATTENTE_EFFET", "IMPAYE",
        "RESILIATION_EN_COURS", "RESILIE", "RENONCE"})
    void rg2EtRg3RefuseLesStatutsQuiBloquentLaRegeneration(String statut) {
        contrat.setStatut(statut);

        assertThatThrownBy(() -> service.regenerer(NUMERO)).isInstanceOf(RegenerationImpossibleException.class);
        verifyNoInteractions(generation, envoi);
    }

    @Test
    void uneRegenerationParMinuteAuPlus() {
        Document regenereeIlYA30s = version(2, true, MAINTENANT.minusSeconds(30));
        regenereeIlYA30s.setMotif(RegenerationContratService.MOTIF);
        when(documents.enVigueurVerrouille(3L, "CONDITIONS_PARTICULIERES", ""))
                .thenReturn(Optional.of(regenereeIlYA30s));

        assertThatThrownBy(() -> service.regenerer(NUMERO)).isInstanceOf(RegenerationTropRapprocheeException.class);
        verify(generation, never()).generer(any());
    }

    @Test
    void uneV1RecenteCreeeALaSignatureNeBloquePasLaPremiereRegeneration() {
        Document v1 = version(1, true, MAINTENANT.minusSeconds(10));
        v1.setMotif(ConditionsParticulieresALaSignature.MOTIF);
        when(documents.enVigueurVerrouille(3L, "CONDITIONS_PARTICULIERES", "")).thenReturn(Optional.of(v1));
        when(generation.generer(any())).thenReturn(version(2, true, MAINTENANT));
        when(generation.telecharger(102L)).thenReturn(new DocumentService.Fichier("cp.pdf", "pdf", PDF));

        assertThat(service.regenerer(NUMERO).version()).isEqualTo(2);
    }

    @Test
    void uneVersionDePlusDUneMinutePermetDeRegenerer() {
        when(documents.enVigueurVerrouille(3L, "CONDITIONS_PARTICULIERES", ""))
                .thenReturn(Optional.of(version(1, true, MAINTENANT.minusSeconds(61))));
        when(generation.generer(any())).thenReturn(version(2, true, MAINTENANT));
        when(generation.telecharger(102L)).thenReturn(new DocumentService.Fichier("cp.pdf", "pdf", PDF));

        assertThat(service.regenerer(NUMERO).version()).isEqualTo(2);
    }

    @Test
    void deuxDemandesSimultaneesLaSecondeEstRefuseeClairement() {
        when(generation.generer(any())).thenThrow(new DataIntegrityViolationException("uk_document_en_vigueur"));

        assertThatThrownBy(() -> service.regenerer(NUMERO)).isInstanceOf(RegenerationTropRapprocheeException.class);
    }

    @Test
    void unContratInconnuRepondIntrouvable() {
        when(contrats.parNumero("AE-X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.regenerer("AE-X")).isInstanceOf(ContratInconnuException.class);
    }

    @Test
    void pdfPasEncoreProduitPasDEmailLaVersionResteCreee() {
        when(generation.generer(any())).thenReturn(version(2, false, MAINTENANT));

        RegenerationResponse reponse = service.regenerer(NUMERO);

        assertThat(reponse.disponible()).isFalse();
        assertThat(reponse.emailEnvoye()).isFalse();
        verifyNoInteractions(envoi);
    }

    @Test
    void ac3UnEchecDEnvoiNeRemetPasEnCauseLaNouvelleVersion() {
        when(generation.generer(any())).thenReturn(version(2, true, MAINTENANT));
        when(generation.telecharger(102L)).thenReturn(new DocumentService.Fichier("cp.pdf", "pdf", PDF));
        doThrow(new MailSendException("SMTP indisponible")).when(envoi)
                .envoyer(anyString(), anyString(), anyString(), anyInt(), anyString(), any());

        RegenerationResponse reponse = service.regenerer(NUMERO);

        assertThat(reponse.version()).isEqualTo(2);
        assertThat(reponse.disponible()).isTrue();
        assertThat(reponse.emailEnvoye()).isFalse();
    }
}
