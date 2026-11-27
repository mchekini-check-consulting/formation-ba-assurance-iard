package net.checkconsulting.assuranceiard.api.service.sinistre;

import static net.checkconsulting.assuranceiard.api.service.sinistre.FixturesSinistre.brouillon;
import static net.checkconsulting.assuranceiard.api.service.sinistre.FixturesSinistre.contrat;
import static net.checkconsulting.assuranceiard.api.service.sinistre.FixturesSinistre.present;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.config.DonneesTestProperties;
import net.checkconsulting.assuranceiard.api.exception.DeclarationDejaEnvoyeeException;
import net.checkconsulting.assuranceiard.api.exception.DocumentAltereException;
import net.checkconsulting.assuranceiard.api.exception.PieceRefuseeException;
import net.checkconsulting.assuranceiard.api.exception.StockageIndisponibleException;
import net.checkconsulting.assuranceiard.api.model.PieceJointe;
import net.checkconsulting.assuranceiard.api.model.PieceJointeContenu;
import net.checkconsulting.assuranceiard.api.model.Sinistre;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.PieceJointeContenuRepository;
import net.checkconsulting.assuranceiard.api.repository.SinistreRepository;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

/**
 * US-24, AC7, AC14 à AC16, RG7 : dépôt des justificatifs, avec les fichiers de test du ticket (photo-degats.jpg,
 * constat.pdf de 12 Mo, facture.exe, erreur-stockage.pdf). Le stockage en base est le vrai bouchon, avec ses règles.
 */
class PiecesJointesSinistreServiceTest {

    static final byte[] PDF = "%PDF-1.7\n1 0 obj\n".getBytes(StandardCharsets.US_ASCII);
    static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F'};
    static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0};

    private final FixturesSinistre fixtures = new FixturesSinistre();
    private final ContratRepository contrats = mock(ContratRepository.class);
    private final SinistreRepository sinistres = mock(SinistreRepository.class);
    private final PieceJointeContenuRepository contenus = mock(PieceJointeContenuRepository.class);
    private final StockagePiecesJointes stockage = new StockagePiecesJointesEnBase(contenus,
            new DonneesTestProperties(true, null));
    private final DeclarationSinistreService declarations = new DeclarationSinistreService(mock(SessionService.class),
            contrats, sinistres, fixtures.referentiel, fixtures.vue, fixtures.json, fixtures.proprietes);
    private final PiecesJointesSinistreService service = new PiecesJointesSinistreService(declarations, sinistres,
            fixtures.pieces, fixtures.referentiel, stockage, fixtures.vue);

    private Sinistre sinistre;

    @BeforeEach
    void preparer() {
        sinistre = brouillon(42L, 3L, "VOL");
        when(sinistres.findById(42L)).thenReturn(Optional.of(sinistre));
        when(contrats.findById(3L)).thenReturn(present(contrat(3L, "AE-RECETTE-A03", "ACTIF", "TR")));
        when(fixtures.pieces.saveAndFlush(any(PieceJointe.class))).thenAnswer(appel -> {
            PieceJointe piece = appel.getArgument(0);
            piece.setId(7L);
            return piece;
        });
    }

    private static MockMultipartFile fichier(String nom, byte[] contenu) {
        return new MockMultipartFile("fichier", nom, "application/octet-stream", contenu);
    }

    @Test
    void ac14UnePhotoAuBonFormatEstStockeeEnBaseAvecSonEmpreinte() {
        service.deposer(42L, "RECEPISSE_PLAINTE", fichier("photo-degats.jpg", JPG));

        ArgumentCaptor<PieceJointe> piece = ArgumentCaptor.forClass(PieceJointe.class);
        verify(fixtures.pieces).save(piece.capture());
        assertThat(piece.getValue().getFormat()).isEqualTo("JPG");
        assertThat(piece.getValue().getTailleOctets()).isEqualTo(JPG.length);
        assertThat(piece.getValue().getEmpreinte()).hasSize(64)
                .isEqualTo(PiecesJointesSinistreService.empreinte(JPG));
        assertThat(piece.getValue().getCleStockage()).isEqualTo("base:piece_jointe_contenu/7");
        ArgumentCaptor<PieceJointeContenu> contenu = ArgumentCaptor.forClass(PieceJointeContenu.class);
        verify(contenus).save(contenu.capture());
        assertThat(contenu.getValue().getContenu()).isEqualTo(JPG);
    }

    @Test
    void ac15UnFichierDePlusDe10MoEstRefuse() {
        byte[] gros = Arrays.copyOf(PDF, 12 * 1024 * 1024);

        assertThatThrownBy(() -> service.deposer(42L, "RECEPISSE_PLAINTE", fichier("constat.pdf", gros)))
                .isInstanceOf(PieceRefuseeException.class).hasMessage(PieceRefuseeException.TAILLE);
        verify(fixtures.pieces, never()).saveAndFlush(any());
    }

    @Test
    void ac15LeFormatEstControleSurLeContenuReelEtPasSeulementLExtension() {
        assertThatThrownBy(() -> service.deposer(42L, "RECEPISSE_PLAINTE", fichier("facture.exe", EXE)))
                .isInstanceOf(PieceRefuseeException.class).hasMessage(PieceRefuseeException.FORMAT);
        assertThatThrownBy(() -> service.deposer(42L, "RECEPISSE_PLAINTE", fichier("facture.pdf", EXE)))
                .isInstanceOf(PieceRefuseeException.class).hasMessage(PieceRefuseeException.FORMAT);
        assertThatThrownBy(() -> service.deposer(42L, "RECEPISSE_PLAINTE", fichier("photo.png", JPG)))
                .isInstanceOf(PieceRefuseeException.class).hasMessage(PieceRefuseeException.FORMAT);
        assertThatThrownBy(() -> service.deposer(42L, "RECEPISSE_PLAINTE", fichier("vide.pdf", new byte[0])))
                .isInstanceOf(PieceRefuseeException.class).hasMessage(PieceRefuseeException.VIDE);
    }

    @Test
    void rg7VingtFichiersAuPlusParDossier() {
        when(fixtures.pieces.countBySinistreIdAndStatut(42L, "STOCKEE")).thenReturn(20L);

        assertThatThrownBy(() -> service.deposer(42L, "RECEPISSE_PLAINTE", fichier("photo.jpg", JPG)))
                .isInstanceOf(PieceRefuseeException.class).hasMessage(PieceRefuseeException.NOMBRE);
    }

    @Test
    void ac7LaPieceDoitEtreUnePieceAttenduePourCeType() {
        assertThatThrownBy(() -> service.deposer(42L, "CONSTAT_AMIABLE", fichier("constat.pdf", PDF)))
                .isInstanceOf(PieceRefuseeException.class).hasMessage(PieceRefuseeException.TYPE_PIECE);
    }

    @Test
    void ac16LeStockageEnErreurDonneUnMessageEtRienNEstStocke() {
        assertThatThrownBy(() -> service.deposer(42L, "RECEPISSE_PLAINTE", fichier("erreur-stockage.pdf", PDF)))
                .isInstanceOf(StockageIndisponibleException.class);
        verify(contenus, never()).save(any());
    }

    @Test
    void ac16LaSimulationDErreurNExisteQuAvecLesDonneesDeTest() {
        StockagePiecesJointes enProduction = new StockagePiecesJointesEnBase(contenus,
                new DonneesTestProperties(false, null));
        PieceJointe piece = new PieceJointe();
        piece.setId(8L);
        piece.setNomFichier("erreur-stockage.pdf");

        assertThat(enProduction.stocker(piece, PDF)).isEqualTo("base:piece_jointe_contenu/8");
    }

    @Test
    void ac7UnePieceDeposeeApresLEnvoiMetAJourPiecesManquantes() {
        sinistre.setStatut("DECLARE");
        sinistre.setPiecesManquantes(true);
        PieceJointe recepisse = new PieceJointe();
        recepisse.setTypePiece("RECEPISSE_PLAINTE");
        recepisse.setTailleOctets(PDF.length);
        when(fixtures.pieces.findBySinistreIdAndStatutOrderByIdAsc(42L, "STOCKEE")).thenReturn(List.of(recepisse));

        assertThat(service.deposer(42L, "RECEPISSE_PLAINTE", fichier("plainte.pdf", PDF)).piecesManquantes())
                .isFalse();
        assertThat(sinistre.getPiecesManquantes()).isFalse();
        verify(sinistres).save(sinistre);
    }

    @Test
    void ac14UnePieceSeSupprimeSeulementDansUnBrouillon() {
        PieceJointe piece = new PieceJointe();
        piece.setId(7L);
        when(fixtures.pieces.findByIdAndSinistreId(7L, 42L)).thenReturn(Optional.of(piece));

        service.supprimer(42L, 7L);
        verify(fixtures.pieces).delete(piece);

        sinistre.setStatut("DECLARE");
        assertThatThrownBy(() -> service.supprimer(42L, 7L)).isInstanceOf(DeclarationDejaEnvoyeeException.class);
    }

    @Test
    void unFichierAltereNEstJamaisRenvoye() {
        PieceJointe piece = new PieceJointe();
        piece.setId(7L);
        piece.setNomFichier("plainte.pdf");
        piece.setFormat("PDF");
        piece.setEmpreinte(PiecesJointesSinistreService.empreinte(PDF));
        when(fixtures.pieces.findByIdAndSinistreId(7L, 42L)).thenReturn(Optional.of(piece));
        PieceJointeContenu contenu = new PieceJointeContenu();
        contenu.setContenu(PDF);
        when(contenus.findByPieceJointeId(7L)).thenReturn(Optional.of(contenu));

        assertThat(service.lire(42L, 7L).contenu()).isEqualTo(PDF);

        PieceJointeContenu altere = new PieceJointeContenu();
        altere.setContenu(JPG);
        when(contenus.findByPieceJointeId(eq(7L))).thenReturn(Optional.of(altere));
        assertThatThrownBy(() -> service.lire(42L, 7L)).isInstanceOf(DocumentAltereException.class);
    }

    @Test
    void leNomAfficheNeGardeNiCheminNiLongueurExcessive() {
        assertThat(PiecesJointesSinistreService.nom("C:\\Users\\moi\\photo.jpg", "jpg")).isEqualTo("photo.jpg");
        assertThat(PiecesJointesSinistreService.nom(" ", "pdf")).isEqualTo("piece.pdf");
        assertThat(PiecesJointesSinistreService.nom("a".repeat(300) + ".pdf", "pdf")).hasSize(255).endsWith(".pdf");
        assertThat(PiecesJointesSinistreService.formatControle("scan.JPEG", JPG)).isEqualTo("jpg");
    }
}
