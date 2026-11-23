package net.checkconsulting.assuranceiard.api.service.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.checkconsulting.assuranceiard.api.config.DocumentsProperties;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionOperations;

class RepriseDocumentsTest {

    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final DocumentsGeneresService service = mock(DocumentsGeneresService.class);
    private final CatalogueModeles catalogue = mock(CatalogueModeles.class);

    private RepriseDocuments reprise(boolean active) {
        DocumentsProperties reglages = new DocumentsProperties(null, null, null, null,
                new DocumentsProperties.Reprise(active, Duration.ofMinutes(5), Duration.ofMinutes(1), 20));
        return new RepriseDocuments(documents, service, catalogue, reglages, TransactionOperations.withoutTransaction());
    }

    @BeforeEach
    void preparer() {
        when(catalogue.typesGeres()).thenReturn(Set.of("CONDITIONS_PARTICULIERES", "CONDITIONS_GENERALES"));
        when(documents.enVigueur(eq(0L), eq("CONDITIONS_GENERALES"), anyString()))
                .thenReturn(Optional.of(new Document()));
    }

    @Test
    void stockeLesDocumentsEnAttenteSansCreerDeVersion() {
        Document premier = new Document();
        Document second = new Document();
        when(documents.aStocker(anyCollection(), eq(20))).thenReturn(List.of(1L, 2L, 3L));
        when(documents.aStockerVerrouille(1L)).thenReturn(Optional.of(premier));
        when(documents.aStockerVerrouille(2L)).thenReturn(Optional.of(second));
        // 3 : déjà pris par l'autre instance du backend.
        when(documents.aStockerVerrouille(3L)).thenReturn(Optional.empty());
        when(service.stocker(premier)).thenReturn(true);
        when(service.stocker(second)).thenReturn(false);

        assertThat(reprise(true).reprendre()).isEqualTo(1);

        verify(service, times(2)).stocker(any());
        verify(service, never()).generer(any());
    }

    @Test
    void creeLesConditionsGeneralesDeChaqueProduitUneSeuleFois() {
        when(documents.enVigueur(0L, "CONDITIONS_GENERALES", "AUTO")).thenReturn(Optional.empty());
        when(documents.enVigueur(0L, "CONDITIONS_GENERALES", "HABITATION")).thenReturn(Optional.empty());
        when(service.generer(DemandeDocument.commun(TypeDocument.CONDITIONS_GENERALES, "HABITATION",
                RepriseDocuments.MOTIF_CG))).thenThrow(new DataIntegrityViolationException("déjà créées"));

        reprise(true).reprendre();

        verify(service).generer(
                DemandeDocument.commun(TypeDocument.CONDITIONS_GENERALES, "AUTO", RepriseDocuments.MOTIF_CG));
        verify(service).generer(
                DemandeDocument.commun(TypeDocument.CONDITIONS_GENERALES, "HABITATION", RepriseDocuments.MOTIF_CG));
    }

    @Test
    void lesConditionsGeneralesExistantesNeSontPasRegenerees() {
        when(documents.aStocker(anyCollection(), anyInt())).thenReturn(List.of());

        assertThat(reprise(true).reprendre()).isZero();

        verify(service, never()).generer(any());
    }

    @Test
    void laTachePlanifieeNeFaitRienSiElleEstArretee() {
        reprise(false).planifiee();

        verifyNoInteractions(documents, service);
    }

    @Test
    void laTachePlanifieeLanceUnPassage() {
        when(documents.aStocker(anyCollection(), anyInt())).thenReturn(List.of());

        reprise(true).planifiee();

        verify(documents).aStocker(Set.of("CONDITIONS_PARTICULIERES", "CONDITIONS_GENERALES"), 20);
    }
}
