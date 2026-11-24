package net.checkconsulting.assuranceiard.api.service.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ConditionsParticulieresALaSignatureTest {

    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final DocumentsGeneresService generation = mock(DocumentsGeneresService.class);
    private final ConditionsParticulieresALaSignature service = new ConditionsParticulieresALaSignature(documents,
            generation, mock(PlatformTransactionManager.class));

    @AfterEach
    void nettoyer() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void ac4CreeLaVersion1ALaSignature() {
        when(documents.enVigueur(42L, "CONDITIONS_PARTICULIERES", "")).thenReturn(Optional.empty());

        service.creerApresSignature(42L);

        verify(generation).generer(DemandeDocument.duContrat(TypeDocument.CONDITIONS_PARTICULIERES, 42L,
                ConditionsParticulieresALaSignature.MOTIF));
    }

    @Test
    void neRecreePasDeVersionSiLeContratEnADeja() {
        when(documents.enVigueur(42L, "CONDITIONS_PARTICULIERES", "")).thenReturn(Optional.of(new Document()));

        service.creerApresSignature(42L);

        verify(generation, never()).generer(any());
    }

    @Test
    void unEchecNeRemontePasLaSignatureResteValable() {
        when(documents.enVigueur(42L, "CONDITIONS_PARTICULIERES", "")).thenReturn(Optional.empty());
        when(generation.generer(any())).thenThrow(new IllegalStateException("base indisponible"));

        assertThatCode(() -> service.creerApresSignature(42L)).doesNotThrowAnyException();
    }

    @Test
    void dansUneTransactionLaCreationAttendLEnregistrementDeLaSignature() {
        when(documents.enVigueur(42L, "CONDITIONS_PARTICULIERES", "")).thenReturn(Optional.empty());
        TransactionSynchronizationManager.initSynchronization();

        service.creerApresSignature(42L);

        verify(generation, never()).generer(any());
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(generation).generer(any());
    }
}
