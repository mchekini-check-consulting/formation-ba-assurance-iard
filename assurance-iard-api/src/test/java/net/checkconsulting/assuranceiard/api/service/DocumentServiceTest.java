package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.DocumentResponse;
import net.checkconsulting.assuranceiard.api.exception.DocumentInconnuException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.model.Justificatif;
import net.checkconsulting.assuranceiard.api.repository.JustificatifRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    private static final UUID REFERENCE = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    @Mock
    private SessionService sessionService;

    @Mock
    private JustificatifRepository justificatifRepository;

    @Mock
    private StockageJustificatifs stockage;

    @InjectMocks
    private DocumentService service;

    private Compte compte;
    private Justificatif justificatif;

    @BeforeEach
    void preparer() {
        compte = new Compte();
        DossierSouscription dossier = new DossierSouscription();
        dossier.setCompte(compte);
        dossier.setReferenceDevis(REFERENCE);
        justificatif = new Justificatif();
        justificatif.setId(5L);
        justificatif.setDossier(dossier);
        justificatif.setCategorie("identite");
        justificatif.setTypeDocument("passeport");
        justificatif.setNomFichier("passeport.pdf");
        justificatif.setTypeFichier("pdf");
        justificatif.setTaille(3);
        justificatif.setCleStockage("cle.pdf");
    }

    @Test
    void ac1ListeLesDocumentsDuClientConnecte() {
        when(sessionService.compteConnecte("Bearer jeton")).thenReturn(compte);
        when(justificatifRepository.findByDossierCompteOrderByIdDesc(compte)).thenReturn(List.of(justificatif));

        assertThat(service.lister("Bearer jeton")).containsExactly(
                new DocumentResponse(5L, REFERENCE, "identite", "passeport", "passeport.pdf", "pdf", 3));
    }

    @Test
    void ouvreLeContenuDuDocument() {
        when(justificatifRepository.findById(5L)).thenReturn(Optional.of(justificatif));
        when(stockage.lire("cle.pdf")).thenReturn(Optional.of(new byte[] {1, 2, 3}));

        DocumentService.Fichier fichier = service.ouvrir(5L);

        assertThat(fichier.nom()).isEqualTo("passeport.pdf");
        assertThat(fichier.type()).isEqualTo("pdf");
        assertThat(fichier.contenu()).containsExactly(1, 2, 3);
    }

    @Test
    void unDocumentAbsentOuEfaceEstIntrouvable() {
        when(justificatifRepository.findById(6L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.ouvrir(6L)).isInstanceOf(DocumentInconnuException.class);

        when(justificatifRepository.findById(5L)).thenReturn(Optional.of(justificatif));
        when(stockage.lire("cle.pdf")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.ouvrir(5L)).isInstanceOf(DocumentInconnuException.class);
    }
}
