package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.config.DocumentsProperties;
import net.checkconsulting.assuranceiard.api.dto.ContratAutoResponse;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.dto.SignatureContratRequest;
import net.checkconsulting.assuranceiard.api.exception.AccesNonAutoriseException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.Profil;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisAutoRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentContenuRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.repository.HistoriqueStatutContratRepository;
import net.checkconsulting.assuranceiard.api.repository.PreuveSignatureRepository;
import net.checkconsulting.assuranceiard.api.service.document.ConditionsParticulieresALaSignature;
import net.checkconsulting.assuranceiard.api.service.document.GabaritPdf;
import net.checkconsulting.assuranceiard.api.service.document.MemoVehiculeALaSignature;
import net.checkconsulting.assuranceiard.api.service.document.ModeleConditionsParticulieresAuto;
import net.checkconsulting.assuranceiard.api.service.fva.FvaService;
import net.checkconsulting.assuranceiard.api.service.hamon.HamonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SignatureAutoServiceTest {

    private static final String JETON = "Bearer jeton";
    /** 15 février 2024, 10h00 UTC = même jour en France. */
    private static final Instant MAINTENANT = Instant.parse("2024-02-15T10:00:00Z");
    private static final LocalDate AUJOURD_HUI = LocalDate.of(2024, 2, 15);

    private final SessionService sessionService = mock(SessionService.class);
    private final ContratRepository contratsRepo = mock(ContratRepository.class);
    private final DevisAutoRepository devisAutos = mock(DevisAutoRepository.class);
    private final DonneesContratRepository donneesContrats = mock(DonneesContratRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final DocumentContenuRepository documentContenus = mock(DocumentContenuRepository.class);
    private final PreuveSignatureRepository preuves = mock(PreuveSignatureRepository.class);
    private final HistoriqueStatutContratRepository historique = mock(HistoriqueStatutContratRepository.class);
    private final CodeSignatureContratService codeSignatureService = mock(CodeSignatureContratService.class);
    private final ConditionsParticulieresALaSignature conditionsParticulieres =
            mock(ConditionsParticulieresALaSignature.class);
    private final MemoVehiculeALaSignature memoVehicule = mock(MemoVehiculeALaSignature.class);
    private final ModeleConditionsParticulieresAuto modeleCpAuto = mock(ModeleConditionsParticulieresAuto.class);
    private final EnvoiEmailContrat emailService = mock(EnvoiEmailContrat.class);
    private final FvaService fvaService = mock(FvaService.class);
    private final HamonService hamonService = mock(HamonService.class);
    private final DocumentsProperties documentsProps = new DocumentsProperties(null, null, null, null, null, null);
    private final Clock horloge = Clock.fixed(MAINTENANT, ZoneId.of("Europe/Paris"));

    private final SignatureAutoService service = new SignatureAutoService(
            sessionService, contratsRepo, devisAutos, donneesContrats, documents, documentContenus,
            preuves, historique, codeSignatureService, conditionsParticulieres, memoVehicule,
            modeleCpAuto, emailService, fvaService, hamonService, documentsProps, horloge);

    private final Compte compte = new Compte();
    private final Contrat contrat = new Contrat();

    @BeforeEach
    void setUp() {
        compte.setId(1L);
        compte.setEmail("jean@test.fr");
        compte.setPrenom("Jean");
        compte.setNom("Dupont");
        compte.setProfil(Profil.PROSPECT);

        contrat.setId(42L);
        contrat.setNumero("AUTO-2024-00042");
        contrat.setStatut("EN_ATTENTE_DE_SIGNATURE");
        contrat.setDateEffet(AUJOURD_HUI.plusDays(30));
        contrat.setCompte(compte);
        contrat.setResiliationHamon(false);

        when(sessionService.compteConnecte(JETON)).thenReturn(compte);
        when(contratsRepo.findByDevisAutoId(10L)).thenReturn(Optional.of(contrat));
        when(contratsRepo.save(contrat)).thenReturn(contrat);
        when(modeleCpAuto.rediger(any())).thenReturn(
                new net.checkconsulting.assuranceiard.api.service.document.ContenuDocument(
                        "CP AUTO", "AUTO-2024-00042",
                        List.of(new net.checkconsulting.assuranceiard.api.service.document.ContenuDocument.Section(
                                "Test", "Ligne test"))));
        when(donneesContrats.lire(42L)).thenReturn(Optional.of(donneesFixture()));
        when(documents.enVigueurDuContrat(anyLong(), any())).thenReturn(List.of());
    }

    // ---- obtenir ----

    @Test
    void obtenirRetourneLeDonneesContrat() {
        ContratAutoResponse rep = service.obtenir(10L, JETON);
        assertThat(rep.numero()).isEqualTo("AUTO-2024-00042");
        assertThat(rep.statut()).isEqualTo("EN_ATTENTE_DE_SIGNATURE");
    }

    @Test
    void obtenirRefuseSiCompteNonProprietaire() {
        Compte autre = new Compte();
        autre.setId(99L);
        when(sessionService.compteConnecte(JETON)).thenReturn(autre);

        assertThatThrownBy(() -> service.obtenir(10L, JETON))
                .isInstanceOf(AccesNonAutoriseException.class);
    }

    // ---- signer : statut selon date d'effet ----

    @Test
    void signerDateFutureTransitionEnAttenteEffet() {
        contrat.setDateEffet(AUJOURD_HUI.plusDays(10));

        service.signer(10L, new SignatureContratRequest("123456"), "1.2.3.4", JETON);

        assertThat(contrat.getStatut()).isEqualTo("EN_ATTENTE_EFFET");
        verify(historique).save(any());
    }

    @Test
    void signerDateAujourdhuiTransitionActifJustificatifs() {
        contrat.setDateEffet(AUJOURD_HUI);

        service.signer(10L, new SignatureContratRequest("123456"), "1.2.3.4", JETON);

        assertThat(contrat.getStatut()).isEqualTo("ACTIF_JUSTIFICATIFS_ATTENDUS");
    }

    @Test
    void signerDatePasseeTransitionActifJustificatifs() {
        contrat.setDateEffet(AUJOURD_HUI.minusDays(1));

        service.signer(10L, new SignatureContratRequest("123456"), "1.2.3.4", JETON);

        assertThat(contrat.getStatut()).isEqualTo("ACTIF_JUSTIFICATIFS_ATTENDUS");
    }

    @Test
    void signerPromouveLeProfil() {
        service.signer(10L, new SignatureContratRequest("123456"), "1.2.3.4", JETON);

        assertThat(compte.getProfil()).isEqualTo(Profil.ASSURE);
    }

    @Test
    void signerNaPromouvePasUnProfilDejaAssure() {
        compte.setProfil(Profil.ASSURE);

        service.signer(10L, new SignatureContratRequest("123456"), "1.2.3.4", JETON);

        assertThat(compte.getProfil()).isEqualTo(Profil.ASSURE);
    }

    @Test
    void signerEnregistreLaPreuveDeSignature() {
        service.signer(10L, new SignatureContratRequest("123456"), "1.2.3.4", JETON);

        verify(preuves).save(any());
    }

    @Test
    void signerLanceCreationCpEtMemo() {
        service.signer(10L, new SignatureContratRequest("123456"), "1.2.3.4", JETON);

        verify(conditionsParticulieres).creerApresSignature(42L);
        verify(memoVehicule).creerApresSignature(42L);
    }

    @Test
    void signerSansHamonNappelleHamonService() {
        contrat.setResiliationHamon(false);

        service.signer(10L, new SignatureContratRequest("123456"), "1.2.3.4", JETON);

        verify(hamonService, never()).resilier(any(Long.class), any(), any(), any());
    }

    // ---- helpers ----

    private DonneesContrat donneesFixture() {
        return new DonneesContrat(42L, "AUTO-2024-00042", "AUTO", "TIERS",
                new DonneesContrat.Souscripteur("M.", "Jean", "Dupont"),
                null,
                new DonneesContrat.Vehicule("AB-123-CD", "Renault", "Clio", null),
                null,
                new DonneesContrat.Tarif(new BigDecimal("300"), new BigDecimal("42.50"),
                        new BigDecimal("510"), AUJOURD_HUI.plusDays(30), null),
                new DonneesContrat.Paiement("MENSUEL", 5, "FR76 •••• 1234", "RUM-001"),
                null,
                List.of());
    }
}
