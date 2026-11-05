package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.IdentiteResponse;
import net.checkconsulting.assuranceiard.api.dto.InformationsRequest;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.exception.InformationRefuseeException;
import net.checkconsulting.assuranceiard.api.mapper.CompteMapper;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.model.Justificatif;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import net.checkconsulting.assuranceiard.api.repository.DossierSouscriptionRepository;
import net.checkconsulting.assuranceiard.api.repository.JustificatifRepository;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import net.checkconsulting.assuranceiard.api.utils.TypesFichier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class InformationsServiceTest {

    private static final String JETON = "Bearer jeton";
    private static final UUID REFERENCE = UUID.randomUUID();
    /** 5 novembre 2026, 23 h 30 UTC : déjà le 6 novembre en France. */
    private static final Instant MAINTENANT = Instant.parse("2026-11-05T23:30:00Z");
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1'};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0};

    private final SessionService sessionService = mock(SessionService.class);
    private final RecommandationDevisRepository recommandations = mock(RecommandationDevisRepository.class);
    private final DossierSouscriptionRepository dossiers = mock(DossierSouscriptionRepository.class);
    private final JustificatifRepository justificatifs = mock(JustificatifRepository.class);
    private final StockageJustificatifs stockage = mock(StockageJustificatifs.class);
    private final InformationsService service = new InformationsService(
            sessionService,
            Mappers.getMapper(CompteMapper.class),
            recommandations,
            dossiers,
            justificatifs,
            stockage,
            Clock.fixed(MAINTENANT, ZoneOffset.UTC));

    private final Compte compte = new Compte();

    @BeforeEach
    void preparer() {
        when(sessionService.compteConnecte(JETON)).thenReturn(compte);
        when(recommandations.findByReference(REFERENCE)).thenReturn(Optional.of(new RecommandationDevis()));
        when(dossiers.findByCompteAndReferenceDevis(compte, REFERENCE)).thenReturn(Optional.empty());
        when(dossiers.save(any())).thenAnswer(appel -> appel.getArgument(0));
        when(justificatifs.findByDossierAndCategorie(any(), anyString())).thenReturn(Optional.empty());
        when(justificatifs.findByDossier(any())).thenReturn(List.of());
        when(stockage.stocker(any(), anyString())).thenReturn("cle-1", "cle-2");
    }

    private InformationsRequest demande(LocalDate naissance) {
        return new InformationsRequest(
                REFERENCE, "madame", " Durand ", " Marie ", naissance, "0612345678",
                "passeport", "gaz", List.of(" Paul Martin "));
    }

    private MultipartFile fichier(String nom, byte[] contenu) {
        return new MockMultipartFile("fichier", nom, null, contenu);
    }

    private void enregistrer(LocalDate naissance, MultipartFile identite, MultipartFile domicile) {
        service.enregistrer(JETON, demande(naissance), identite, domicile);
    }

    private void enregistrerAvecIdentite(MultipartFile identite) {
        enregistrer(LocalDate.of(1990, 1, 1), identite, fichier("facture.jpg", JPG));
    }

    @Test
    void lireRenvoieLIdentiteDuCompte() {
        compte.setNom("Durand");
        compte.setTelephone("0612345678");

        IdentiteResponse identite = service.lire(JETON);

        assertThat(identite.nom()).isEqualTo("Durand");
        assertThat(identite.telephone()).isEqualTo("0612345678");
        assertThat(identite.civilite()).isNull();
    }

    @Test
    void enregistreLIdentiteDansLeCompte() {
        enregistrerAvecIdentite(fichier("carte.pdf", PDF));

        assertThat(compte.getCivilite()).isEqualTo("madame");
        assertThat(compte.getNom()).isEqualTo("Durand");
        assertThat(compte.getPrenom()).isEqualTo("Marie");
        assertThat(compte.getDateNaissance()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(compte.getTelephone()).isEqualTo("0612345678");
    }

    @Test
    void enregistreLeDossierEtSesDeuxJustificatifs() {
        enregistrerAvecIdentite(fichier("carte.pdf", PDF));

        verify(dossiers).save(org.mockito.ArgumentMatchers.argThat((DossierSouscription dossier) ->
                dossier.getCompte() == compte
                        && REFERENCE.equals(dossier.getReferenceDevis())
                        && dossier.getColocataires().equals(List.of("Paul Martin"))
                        && MAINTENANT.equals(dossier.getDateCreation())));
        verify(stockage).stocker(PDF, "pdf");
        verify(stockage).stocker(JPG, "jpg");
        verify(justificatifs).save(org.mockito.ArgumentMatchers.argThat((Justificatif j) ->
                "identite".equals(j.getCategorie())
                        && "passeport".equals(j.getTypeDocument())
                        && "carte.pdf".equals(j.getNomFichier())
                        && "pdf".equals(j.getTypeFichier())
                        && j.getTaille() == PDF.length
                        && "cle-1".equals(j.getCleStockage())));
        verify(justificatifs).save(org.mockito.ArgumentMatchers.argThat((Justificatif j) ->
                "domicile".equals(j.getCategorie()) && "cle-2".equals(j.getCleStockage())));
    }

    @Test
    void unNouveauDepotRemplaceLesFichiersPrecedents() {
        Justificatif ancien = new Justificatif();
        ancien.setCleStockage("ancienne-cle");
        when(justificatifs.findByDossier(any())).thenReturn(List.of(ancien));
        when(justificatifs.findByDossierAndCategorie(any(), eq("identite"))).thenReturn(Optional.of(ancien));

        enregistrerAvecIdentite(fichier("carte.pdf", PDF));

        assertThat(ancien.getCleStockage()).isEqualTo("cle-1");
        verify(stockage).supprimer("ancienne-cle");
        verify(stockage, never()).supprimer("cle-1");
    }

    @Test
    void neLaissePasDeFichierDerriereUnEchecDeStockage() {
        when(stockage.stocker(any(), anyString()))
                .thenReturn("cle-1")
                .thenThrow(new UncheckedIOException(new IOException("disque plein")));

        assertThatThrownBy(() -> enregistrerAvecIdentite(fichier("carte.pdf", PDF)))
                .isInstanceOf(UncheckedIOException.class);

        verify(stockage).supprimer("cle-1");
    }

    @ParameterizedTest
    @CsvSource({"2008-11-06, true", "2008-11-07, false", "2008-11-05, true", "2020-01-01, false"})
    void rg1MajeurLeJourDeSes18AnsEnFrance(LocalDate naissance, boolean accepte) {
        MultipartFile identite = fichier("carte.pdf", PDF);
        MultipartFile domicile = fichier("facture.jpg", JPG);

        if (accepte) {
            assertThatCode(() -> enregistrer(naissance, identite, domicile)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> enregistrer(naissance, identite, domicile))
                    .isInstanceOfSatisfying(InformationRefuseeException.class, erreur ->
                            assertThat(erreur.getChamp()).isEqualTo("dateNaissance"));
        }
    }

    @Test
    void rg4AccepteUnFichierDeCinqMoExactement() {
        byte[] contenu = new byte[(int) TypesFichier.TAILLE_MAX];
        System.arraycopy(PDF, 0, contenu, 0, PDF.length);

        assertThatCode(() -> enregistrerAvecIdentite(fichier("carte.pdf", contenu))).doesNotThrowAnyException();
    }

    @Test
    void rg4RefuseUnFichierDePlusDeCinqMo() {
        byte[] contenu = new byte[(int) TypesFichier.TAILLE_MAX + 1];
        System.arraycopy(PDF, 0, contenu, 0, PDF.length);

        assertThatThrownBy(() -> enregistrerAvecIdentite(fichier("carte.pdf", contenu)))
                .isInstanceOfSatisfying(InformationRefuseeException.class, erreur -> {
                    assertThat(erreur.getChamp()).isEqualTo(InformationsService.CHAMP_PIECE_IDENTITE);
                    assertThat(erreur.getMessage()).contains("dépasse 5 Mo", "PDF, JPG ou PNG");
                });
    }

    @Test
    void rg4RefuseUnFauxPdf() {
        assertThatThrownBy(() -> enregistrerAvecIdentite(fichier("carte.pdf", new byte[] {'M', 'Z', 0, 0, 0})))
                .isInstanceOfSatisfying(InformationRefuseeException.class, erreur ->
                        assertThat(erreur.getMessage()).contains("format", "PDF, JPG ou PNG"));
    }

    @Test
    void rg4RefuseUnVraiPdfAvecUneMauvaiseExtension() {
        assertThatThrownBy(() -> enregistrerAvecIdentite(fichier("carte.exe", PDF)))
                .isInstanceOf(InformationRefuseeException.class);
    }

    @Test
    void indiqueQuelleZoneEstConcernee() {
        LocalDate naissance = LocalDate.of(1990, 1, 1);
        MultipartFile bon = fichier("carte.pdf", PDF);
        MultipartFile mauvais = fichier("facture.pdf", new byte[] {1, 2, 3, 4, 5});

        assertThatThrownBy(() -> enregistrer(naissance, bon, mauvais))
                .isInstanceOfSatisfying(InformationRefuseeException.class, erreur ->
                        assertThat(erreur.getChamp()).isEqualTo(InformationsService.CHAMP_JUSTIFICATIF_DOMICILE));
        verify(stockage, never()).stocker(any(), anyString());
    }

    @Test
    void signaleUnFichierIllisible() throws IOException {
        MultipartFile illisible = mock(MultipartFile.class);
        when(illisible.getBytes()).thenThrow(new IOException("illisible"));

        assertThatThrownBy(() -> enregistrerAvecIdentite(illisible)).isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void refuseUnDevisInconnu() {
        when(recommandations.findByReference(REFERENCE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enregistrerAvecIdentite(fichier("carte.pdf", PDF)))
                .isInstanceOf(DevisInconnuException.class);
        assertThat(compte.getNom()).isNull();
    }
}
