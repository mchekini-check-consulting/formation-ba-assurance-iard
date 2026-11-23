package net.checkconsulting.assuranceiard.api.securite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import org.junit.jupiter.api.Test;

class ProprietairesDocumentContratTest {

    private final DocumentRepository repository = mock(DocumentRepository.class);
    private final ProprietairesDocumentContrat proprietaires = new ProprietairesDocumentContrat(repository);

    @Test
    void leProprietaireEstLeClientDuContratDuDocument() {
        when(repository.proprietaires(12L)).thenReturn(List.of(4L));

        assertThat(proprietaires.type()).isEqualTo(TypeRessource.DOCUMENT_CONTRAT);
        assertThat(proprietaires.proprietaires("12")).containsExactly(4L);
    }

    @Test
    void unIdentifiantMalFormeNeDesigneAucunDocument() {
        assertThat(proprietaires.proprietaires("abc")).isEmpty();
        assertThat(proprietaires.proprietaires(null)).isEmpty();
    }
}
