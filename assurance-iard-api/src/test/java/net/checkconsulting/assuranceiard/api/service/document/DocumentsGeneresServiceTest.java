package net.checkconsulting.assuranceiard.api.service.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.config.DocumentsProperties;
import net.checkconsulting.assuranceiard.api.exception.DocumentAltereException;
import net.checkconsulting.assuranceiard.api.exception.DocumentIndisponibleException;
import net.checkconsulting.assuranceiard.api.exception.DocumentInconnuException;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.DocumentContenu;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DocumentContenuRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.service.DocumentService.Fichier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.util.unit.DataSize;

class DocumentsGeneresServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-23T10:00:00Z"), ZoneOffset.UTC);

    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final DocumentContenuRepository contenus = mock(DocumentContenuRepository.class);
    private final DonneesContratRepository contrats = mock(DonneesContratRepository.class);
    private final List<Document> enregistres = new ArrayList<>();
    private ModeleDocument modele;
    private DocumentsGeneresService service;

    @BeforeEach
    void preparer() {
        modele = mock(ModeleDocument.class);
        when(modele.type()).thenReturn(TypeDocument.CONDITIONS_PARTICULIERES);
        when(modele.convientA(any())).thenReturn(true);
        when(modele.rediger(any())).thenReturn(new ContenuDocument("Conditions particulières", "AE-2026-000001",
                List.of(new ContenuDocument.Section("Franchise", "300,00 €"))));
        service = service(ModelesDocumentsTest.REGLAGES);
        when(documents.saveAndFlush(any())).thenAnswer(appel -> {
            Document document = appel.getArgument(0);
            if (document.getId() == null) {
                document.setId(100L + enregistres.size());
            }
            enregistres.add(document);
            return document;
        });
        when(contenus.findByDocumentId(any())).thenReturn(Optional.empty());
    }

    private DocumentsGeneresService service(DocumentsProperties reglages) {
        return new DocumentsGeneresService(documents, contenus, contrats, new CatalogueModeles(List.of(modele)),
                new GabaritPdf(), reglages, HORLOGE);
    }

    private static Document existant(int version) {
        Document document = ModelesDocumentsTest.document(TypeDocument.CONDITIONS_PARTICULIERES, 7L, null, version);
        document.setStatut("EN_VIGUEUR");
        return document;
    }

    @Test
    void laPremiereVersionEstCreeeEtStockeeAvecSonEmpreinte() {
        when(documents.enVigueurVerrouille(7L, "CONDITIONS_PARTICULIERES", "")).thenReturn(Optional.empty());

        Document document = service.generer(
                DemandeDocument.duContrat(TypeDocument.CONDITIONS_PARTICULIERES, 7L, "Signature"));

        assertThat(document.getVersion()).isEqualTo(1);
        assertThat(document.getStatut()).isEqualTo("EN_VIGUEUR");
        assertThat(document.getMotif()).isEqualTo("Signature");
        assertThat(document.getStocke()).isTrue();
        assertThat(document.getCleStockage()).isEqualTo("base:document_contenu/100");
        ArgumentCaptor<DocumentContenu> contenu = ArgumentCaptor.forClass(DocumentContenu.class);
        verify(contenus).save(contenu.capture());
        assertThat(contenu.getValue().getDocumentId()).isEqualTo(100L);
        assertThat(contenu.getValue().getTailleOctets()).isEqualTo(contenu.getValue().getContenu().length);
        assertThat(document.getEmpreinte()).hasSize(64)
                .isEqualTo(DocumentsGeneresService.empreinte(contenu.getValue().getContenu()));
    }

    @Test
    void laRegenerationRemplaceLaVersionNAvantDeCreerLaVersionNPlus1() {
        Document v2 = existant(2);
        when(documents.enVigueurVerrouille(7L, "CONDITIONS_PARTICULIERES", "")).thenReturn(Optional.of(v2));

        Document v3 = service.generer(
                DemandeDocument.duContrat(TypeDocument.CONDITIONS_PARTICULIERES, 7L, "Avenant"));

        assertThat(v2.getStatut()).isEqualTo("REMPLACE");
        assertThat(v3.getVersion()).isEqualTo(3);
        assertThat(v3.getStatut()).isEqualTo("EN_VIGUEUR");
        assertThat(enregistres).containsExactly(v2, v3);
        InOrder ordre = inOrder(documents);
        ordre.verify(documents).saveAndFlush(v2);
        ordre.verify(documents).saveAndFlush(v3);
    }

    @Test
    void unDocumentCommunEstChercheParSaReference() {
        when(documents.enVigueurVerrouille(0L, "CONDITIONS_PARTICULIERES", "AUTO")).thenReturn(Optional.empty());

        Document document = service.generer(
                DemandeDocument.commun(TypeDocument.CONDITIONS_PARTICULIERES, "AUTO", "Création"));

        assertThat(document.getContratId()).isNull();
        assertThat(document.getReference()).isEqualTo("AUTO");
    }

    @Test
    void unEchecDeProductionNeBloquePasLActionDuClient() {
        when(documents.enVigueurVerrouille(7L, "CONDITIONS_PARTICULIERES", "")).thenReturn(Optional.empty());
        when(modele.rediger(any())).thenThrow(new IllegalStateException("Contrat introuvable"));

        Document document = service.generer(
                DemandeDocument.duContrat(TypeDocument.CONDITIONS_PARTICULIERES, 7L, "Signature"));

        assertThat(document.getId()).isNotNull();
        assertThat(document.getStocke()).isFalse();
        assertThat(document.getCleStockage()).isNull();
        assertThat(document.getDateModification()).isEqualTo(HORLOGE.instant());
        verify(contenus, never()).save(any());
    }

    @Test
    void sansModeleLeDocumentResteNonStocke() {
        Document facture = ModelesDocumentsTest.document(TypeDocument.FACTURE, 7L, "FAC-1", 1);

        assertThat(service.stocker(facture)).isFalse();
        assertThat(facture.getStocke()).isFalse();
    }

    @Test
    void unPdfTropVolumineuxNEstPasStocke() {
        DocumentsProperties petit = new DocumentsProperties(ModelesDocumentsTest.REGLAGES.assureur(),
                ModelesDocumentsTest.REGLAGES.garanties(), "", DataSize.ofBytes(100), null);
        Document document = existant(1);

        assertThat(service(petit).stocker(document)).isFalse();
        verify(contenus, never()).save(any());
    }

    @Test
    void leTelechargementVerifieLEmpreinte() {
        byte[] pdf = {1, 2, 3};
        Document document = existant(2);
        document.setStocke(true);
        document.setEmpreinte(DocumentsGeneresService.empreinte(pdf));
        DocumentContenu contenu = new DocumentContenu();
        contenu.setContenu(pdf);
        when(documents.findById(10L)).thenReturn(Optional.of(document));
        when(contenus.findByDocumentId(10L)).thenReturn(Optional.of(contenu));
        when(contrats.numero(7L)).thenReturn(Optional.of("AE-2026-000001"));

        Fichier fichier = service.telecharger(10L);

        assertThat(fichier).isEqualTo(new Fichier("conditions-particulieres-AE-2026-000001-v2.pdf", "pdf", pdf));

        document.setEmpreinte(DocumentsGeneresService.empreinte(new byte[] {9}));
        assertThatThrownBy(() -> service.telecharger(10L)).isInstanceOf(DocumentAltereException.class);
    }

    /** Génère une version, puis renvoie son PDF stocké (pour relire ses propriétés). */
    private Document genererEtStocker() {
        when(documents.enVigueurVerrouille(7L, "CONDITIONS_PARTICULIERES", "")).thenReturn(Optional.empty());
        Document document = service.generer(
                DemandeDocument.duContrat(TypeDocument.CONDITIONS_PARTICULIERES, 7L, "DEMANDE_CLIENT"));
        ArgumentCaptor<DocumentContenu> contenu = ArgumentCaptor.forClass(DocumentContenu.class);
        verify(contenus).save(contenu.capture());
        when(contenus.findByDocumentId(document.getId())).thenReturn(Optional.of(contenu.getValue()));
        return document;
    }

    @Test
    void us21LaDateDeProductionDuPdfEstRetenue() {
        Document document = genererEtStocker();

        assertThat(service.production(document)).hasValueSatisfying(production ->
                assertThat(production.produitLe()).isEqualTo(HORLOGE.instant().truncatedTo(ChronoUnit.SECONDS)));
    }

    @Test
    void us21DonneesInchangeesLeContenuEstAJour() {
        Document document = genererEtStocker();

        assertThat(service.contenuAJour(document)).isTrue();
    }

    @Test
    void us21UneDonneeChangeeLeContenuNEstPlusAJour() {
        Document document = genererEtStocker();
        when(modele.rediger(any())).thenReturn(new ContenuDocument("Conditions particulières", "AE-2026-000001",
                List.of(new ContenuDocument.Section("Franchise", "500,00 €"))));

        assertThat(service.contenuAJour(document)).isFalse();
    }

    @Test
    void us21UnDocumentNonStockeOuSansEmpreinteNEstPasAJour() {
        Document nonStocke = existant(1);
        assertThat(service.contenuAJour(nonStocke)).isFalse();
        assertThat(service.production(nonStocke)).isEmpty();

        Document ancien = existant(1);
        ancien.setId(20L);
        ancien.setStocke(true);
        DocumentContenu contenu = new DocumentContenu();
        contenu.setContenu(new GabaritPdf().rendre(new ContenuDocument("T", "AE-1", List.of()),
                new GabaritPdf.Pied("T", "AE-1", 1, java.time.LocalDate.of(2026, 1, 1))));
        when(contenus.findByDocumentId(20L)).thenReturn(Optional.of(contenu));
        // Contenu différent : pas à jour (un PDF d'avant US-21 serait aussi « pas à jour », faute d'empreinte).
        assertThat(service.contenuAJour(ancien)).isFalse();
    }

    @Test
    void unContenuDisparuEstUnDocumentAltere() {
        Document document = existant(1);
        document.setStocke(true);
        document.setEmpreinte("abc");
        when(documents.findById(10L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.telecharger(10L)).isInstanceOf(DocumentAltereException.class);
    }

    @Test
    void unDocumentNonStockeOuInconnuNeSeTelechargePas() {
        when(documents.findById(10L)).thenReturn(Optional.of(existant(1)));
        when(documents.findById(11L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.telecharger(10L)).isInstanceOf(DocumentIndisponibleException.class);
        assertThatThrownBy(() -> service.telecharger(11L)).isInstanceOf(DocumentInconnuException.class);
    }

    @Test
    void leNomDuFichierDUnDocumentCommunReprendSaReference() {
        byte[] pdf = {4};
        Document cg = ModelesDocumentsTest.document(TypeDocument.CONDITIONS_GENERALES, null, "AUTO", 1);
        cg.setStocke(true);
        cg.setEmpreinte(DocumentsGeneresService.empreinte(pdf));
        DocumentContenu contenu = new DocumentContenu();
        contenu.setContenu(pdf);
        when(documents.findById(10L)).thenReturn(Optional.of(cg));
        when(contenus.findByDocumentId(10L)).thenReturn(Optional.of(contenu));

        assertThat(service.telecharger(10L).nom()).isEqualTo("conditions-generales-AUTO-v1.pdf");
    }
}
