package net.checkconsulting.assuranceiard.api.securite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisEnregistreRepository;
import net.checkconsulting.assuranceiard.api.repository.DossierSouscriptionRepository;
import net.checkconsulting.assuranceiard.api.repository.JustificatifRepository;
import org.junit.jupiter.api.Test;

class ProprietairesTest {

    private static final UUID REFERENCE = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    @Test
    void devis() {
        DevisEnregistreRepository repository = mock(DevisEnregistreRepository.class);
        when(repository.proprietaires(REFERENCE)).thenReturn(List.of(1L, 1L));
        ProprietairesDevis devis = new ProprietairesDevis(repository);

        assertThat(devis.proprietaires(REFERENCE.toString())).containsExactly(1L);
        assertThat(devis.proprietaires("pas-une-reference")).isEmpty();
        assertThat(devis.proprietaires(null)).isEmpty();
    }

    @Test
    void souscription() {
        DossierSouscriptionRepository repository = mock(DossierSouscriptionRepository.class);
        when(repository.proprietaires(REFERENCE)).thenReturn(List.of(2L));

        assertThat(new ProprietairesSouscription(repository).proprietaires(REFERENCE.toString())).containsExactly(2L);
    }

    @Test
    void contrat() {
        ContratRepository repository = mock(ContratRepository.class);
        when(repository.proprietaires(REFERENCE)).thenReturn(List.of(3L));

        assertThat(new ProprietairesContrat(repository).proprietaires(REFERENCE.toString())).containsExactly(3L);
    }

    @Test
    void document() {
        JustificatifRepository repository = mock(JustificatifRepository.class);
        when(repository.proprietaires(42L)).thenReturn(List.of(4L));
        ProprietairesDocument document = new ProprietairesDocument(repository);

        assertThat(document.proprietaires("42")).containsExactly(4L);
        assertThat(document.proprietaires("abc")).isEmpty();
    }

    @Test
    void unIdentifiantMalFormeNeDesigneAucuneRessource() {
        JustificatifRepository repository = mock(JustificatifRepository.class);

        assertThat(new ProprietairesDocument(repository).proprietaires(null)).isEmpty();
        verifyNoInteractions(repository);
    }
}
