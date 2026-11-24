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
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.RegenerationResponse;
import net.checkconsulting.assuranceiard.api.exception.ContratInconnuException;
import net.checkconsulting.assuranceiard.api.exception.RegenerationImpossibleException;
import net.checkconsulting.assuranceiard.api.exception.RegenerationInutileException;
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
import org.junit.jupiter.params.provider.EnumSource;
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
        when(contrats.findById(3L)).thenReturn(Optional.of(contrat));
        when(documents.enVigueurVerrouille(3L, "CONDITIONS_PARTICULIERES", "")).thenReturn(Optional.empty());
        when(generation.generer(any())).thenReturn(version(2, true, MAINTENANT, null));
        when(generation.telecharger(102L)).thenReturn(new DocumentService.Fichier("cp-v2.pdf", "pdf", PDF));
    }

    private static Document version(int numero, boolean stocke, Instant creation, String motif) {
        Document document = new Document();
        document.setId(100L + numero);
        document.setVersion(numero);
        document.setStocke(stocke);
        document.setDateCreation(creation);
        document.setMotif(motif);
        return document;
    }

    private void enVigueur(Document document) {
        when(documents.enVigueurVerrouille(3L, "CONDITIONS_PARTICULIERES", "")).thenReturn(Optional.of(document));
    }

    @Test
    void ac1EtAc3LaDemandeDuClientRegenereAvecLeMotifDemandeClientEtEnvoieLePdf() {
        RegenerationResponse reponse = service.regenererALaDemandeDuClient(NUMERO);

        assertThat(reponse).isEqualTo(new RegenerationResponse(102L, 2, MAINTENANT, true, true));
        ArgumentCaptor<DemandeDocument> demande = ArgumentCaptor.forClass(DemandeDocument.class);
        verify(generation).generer(demande.capture());
        assertThat(demande.getValue()).isEqualTo(
                DemandeDocument.duContrat(TypeDocument.CONDITIONS_PARTICULIERES, 3L, "DEMANDE_CLIENT"));
        verify(envoi).envoyer("camille@exemple.fr", "Camille", NUMERO, 2, "cp-v2.pdf", PDF);
    }

    @ParameterizedTest
    @EnumSource(value = MotifRegeneration.class, names = {"AVENANT", "COORDONNEES", "CONDITIONS_GENERALES"})
    void laMethodeReutilisableEnregistreLeMotifDuDeclencheur(MotifRegeneration motif) {
        service.regenerer(3L, motif);

        verify(generation).generer(DemandeDocument.duContrat(TypeDocument.CONDITIONS_PARTICULIERES, 3L,
                motif.name()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"EN_ATTENTE_EFFET", "ACTIF_JUSTIFICATIFS_ATTENDUS", "AVENANT_EN_ATTENTE", "IMPAYE",
        "RESILIATION_EN_COURS"})
    void rg2ToutContratSigneEtEnVigueurSaufSuspenduPeutEtreRegenere(String statut) {
        contrat.setStatut(statut);

        assertThat(service.regenererALaDemandeDuClient(NUMERO).version()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUSPENDU", "EN_ATTENTE_DE_SIGNATURE", "RESILIE", "RENONCE"})
    void rg2PasDeRegenerationSurUnContratSuspenduPasEncoreSigneOuPlusEnVigueur(String statut) {
        contrat.setStatut(statut);

        assertThatThrownBy(() -> service.regenererALaDemandeDuClient(NUMERO))
                .isInstanceOf(RegenerationImpossibleException.class);
        verify(generation, never()).generer(any());
        verifyNoInteractions(envoi);
    }

    @Test
    void uneRegenerationDuClientParMinuteAuPlus() {
        enVigueur(version(2, true, MAINTENANT.minusSeconds(30), "DEMANDE_CLIENT"));

        assertThatThrownBy(() -> service.regenererALaDemandeDuClient(NUMERO))
                .isInstanceOf(RegenerationTropRapprocheeException.class);
        verify(generation, never()).generer(any());
    }

    @Test
    void uneV1RecenteCreeeALaSignatureNeBloquePasLaPremiereDemande() {
        enVigueur(version(1, true, MAINTENANT.minusSeconds(10), "SIGNATURE"));

        assertThat(service.regenererALaDemandeDuClient(NUMERO).version()).isEqualTo(2);
    }

    @Test
    void sansChangementDeDonneesLaDemandeDuClientEstRefusee() {
        Document v1 = version(1, true, MAINTENANT.minusSeconds(3600), "DEMANDE_CLIENT");
        enVigueur(v1);
        when(generation.contenuAJour(v1)).thenReturn(true);

        assertThatThrownBy(() -> service.regenererALaDemandeDuClient(NUMERO))
                .isInstanceOf(RegenerationInutileException.class)
                .hasMessageContaining("version 1");
        verify(generation, never()).generer(any());
    }

    @Test
    void desQuUneDonneeAChangeLaDemandeDuClientEstAcceptee() {
        Document v1 = version(1, true, MAINTENANT.minusSeconds(3600), "DEMANDE_CLIENT");
        enVigueur(v1);
        when(generation.contenuAJour(v1)).thenReturn(false);

        assertThat(service.regenererALaDemandeDuClient(NUMERO).version()).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(value = MotifRegeneration.class, names = {"AVENANT", "COORDONNEES", "CONDITIONS_GENERALES"})
    void unDeclenchementAutomatiqueNEstBloqueNiParLaMinuteNiParLesDonneesInchangees(MotifRegeneration motif) {
        Document recente = version(1, true, MAINTENANT.minusSeconds(5), "DEMANDE_CLIENT");
        enVigueur(recente);
        when(generation.contenuAJour(recente)).thenReturn(true);
        contrat.setStatut("SUSPENDU");

        assertThat(service.regenerer(3L, motif).version()).isEqualTo(2);
        verify(generation, never()).contenuAJour(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESILIE", "RENONCE"})
    void unDeclenchementAutomatiqueNeConcernePasUnContratPlusEnVigueur(String statut) {
        contrat.setStatut(statut);

        for (MotifRegeneration motif : List.of(MotifRegeneration.AVENANT, MotifRegeneration.COORDONNEES,
                MotifRegeneration.CONDITIONS_GENERALES)) {
            assertThatThrownBy(() -> service.regenerer(3L, motif)).isInstanceOf(RegenerationImpossibleException.class);
        }
        verify(generation, never()).generer(any());
    }

    @Test
    void estRegenerableIndiqueAuxAutresUsCeQuiEstPossible() {
        assertThat(RegenerationContratService.estRegenerable("ACTIF", MotifRegeneration.DEMANDE_CLIENT)).isTrue();
        assertThat(RegenerationContratService.estRegenerable("SUSPENDU", MotifRegeneration.DEMANDE_CLIENT)).isFalse();
        assertThat(RegenerationContratService.estRegenerable("SUSPENDU", MotifRegeneration.AVENANT)).isTrue();
        assertThat(RegenerationContratService.estRegenerable("RESILIE", MotifRegeneration.AVENANT)).isFalse();
        assertThat(RegenerationContratService.estRegenerable("RENONCE", MotifRegeneration.CONDITIONS_GENERALES))
                .isFalse();
    }

    @Test
    void deuxDemandesSimultaneesLaSecondeEstRefuseeClairement() {
        when(generation.generer(any())).thenThrow(new DataIntegrityViolationException("uk_document_en_vigueur"));

        assertThatThrownBy(() -> service.regenererALaDemandeDuClient(NUMERO))
                .isInstanceOf(RegenerationTropRapprocheeException.class);
    }

    @Test
    void unContratInconnuRepondIntrouvable() {
        when(contrats.parNumero("AE-X")).thenReturn(Optional.empty());
        when(contrats.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.regenererALaDemandeDuClient("AE-X"))
                .isInstanceOf(ContratInconnuException.class);
        assertThatThrownBy(() -> service.regenerer(99L, MotifRegeneration.AVENANT))
                .isInstanceOf(ContratInconnuException.class);
    }

    @Test
    void pdfPasEncoreProduitPasDEmailLaVersionResteCreee() {
        when(generation.generer(any())).thenReturn(version(2, false, MAINTENANT, null));

        RegenerationResponse reponse = service.regenererALaDemandeDuClient(NUMERO);

        assertThat(reponse.disponible()).isFalse();
        assertThat(reponse.emailEnvoye()).isFalse();
        verifyNoInteractions(envoi);
    }

    @Test
    void ac3UnEchecDEnvoiNeRemetPasEnCauseLaNouvelleVersion() {
        doThrow(new MailSendException("SMTP indisponible")).when(envoi)
                .envoyer(anyString(), anyString(), anyString(), anyInt(), anyString(), any());

        RegenerationResponse reponse = service.regenererALaDemandeDuClient(NUMERO);

        assertThat(reponse.version()).isEqualTo(2);
        assertThat(reponse.disponible()).isTrue();
        assertThat(reponse.emailEnvoye()).isFalse();
    }

    @Test
    void seulLeMotifDemandeClientEstSoumisAuxReglesDuClient() {
        assertThat(MotifRegeneration.DEMANDE_CLIENT.demandeDuClient()).isTrue();
        assertThat(MotifRegeneration.AVENANT.demandeDuClient()).isFalse();
        assertThat(MotifRegeneration.SIGNATURE.demandeDuClient()).isFalse();
    }
}
