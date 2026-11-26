package net.checkconsulting.assuranceiard.api.service.avenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.AvenantResponse;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.dto.GarantiesContratResponse;
import net.checkconsulting.assuranceiard.api.dto.PreparationAvenantRequest;
import net.checkconsulting.assuranceiard.api.dto.ProjetAvenantResponse;
import net.checkconsulting.assuranceiard.api.exception.AvenantInconnuException;
import net.checkconsulting.assuranceiard.api.exception.AvenantInvalideException;
import net.checkconsulting.assuranceiard.api.exception.ModificationGarantiesImpossibleException;
import net.checkconsulting.assuranceiard.api.model.Avenant;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.HistoriqueStatutContrat;
import net.checkconsulting.assuranceiard.api.repository.AvenantLigneRepository;
import net.checkconsulting.assuranceiard.api.repository.AvenantRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.repository.HistoriqueStatutContratRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/** US-23 : voir ses garanties, préparer et abandonner un avenant (AC1, AC2, AC3, AC5, AC6). */
class AvenantServiceTest {

    private static final String NUMERO = "AE-RECETTE-A03";
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 26);

    private final ContratRepository contrats = mock(ContratRepository.class);
    private final DonneesContratRepository donneesContrat = mock(DonneesContratRepository.class);
    private final AvenantRepository avenants = mock(AvenantRepository.class);
    private final AvenantLigneRepository lignes = mock(AvenantLigneRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final HistoriqueStatutContratRepository historique = mock(HistoriqueStatutContratRepository.class);
    private final CatalogueGaranties catalogue = mock(CatalogueGaranties.class);
    private final HistoriqueAvenants historiqueAvenants = new HistoriqueAvenants(avenants, lignes,
            HistoriqueAvenantsTest.HORLOGE);
    private final ProjetsAvenant projets = mock(ProjetsAvenant.class);
    private final AvenantService service = new AvenantService(contrats, donneesContrat, avenants, lignes, documents,
            historique, catalogue, historiqueAvenants, projets, HistoriqueAvenantsTest.HORLOGE);

    private Contrat contrat;
    private final DonneesContrat donnees = CatalogueGarantiesTest.donnees(Contrat.AUTO, List.of());

    static final List<GarantieProposee> GARANTIES = List.of(
            new GarantieProposee("RC", "Responsabilité civile", new BigDecimal("12.00"), true, true,
                    CatalogueGaranties.RAISON_RESPONSABILITE_CIVILE),
            new GarantieProposee("PROTECTION_JURIDIQUE", "Protection juridique", new BigDecimal("2.50"), true, false,
                    null),
            new GarantieProposee("ASSISTANCE_0KM", "Assistance dès 0 km", new BigDecimal("3.50"), false, false, null));

    @BeforeEach
    void preparer() {
        contrat = CatalogueGarantiesTest.contrat(Contrat.AUTO, "TR");
        contrat.setStatut("ACTIF");
        contrat.setCotisationMensuelle(new BigDecimal("47.50"));
        contrat.setCotisationAnnuelle(new BigDecimal("570.00"));
        Compte compte = new Compte();
        compte.setPrenom("Camille");
        compte.setNom("Durand");
        org.springframework.test.util.ReflectionTestUtils.setField(contrat, "compte", compte);
        when(contrats.parNumero(NUMERO)).thenReturn(Optional.of(contrat));
        when(donneesContrat.lire(3L)).thenReturn(Optional.of(donnees));
        when(catalogue.garanties(contrat, donnees, AUJOURDHUI)).thenReturn(GARANTIES);
        when(avenants.prochainNumero(3L)).thenReturn(1);
        when(avenants.saveAndFlush(any(Avenant.class))).thenAnswer(appel -> {
            Avenant avenant = appel.getArgument(0);
            if (avenant.getId() == null) {
                avenant.setId(9L);
            }
            return avenant;
        });
        when(projets.projet(any(), any(), any())).thenAnswer(appel -> new ProjetAvenantResponse(appel.getArgument(2),
                BigDecimal.ZERO, BigDecimal.ZERO, "MENSUEL", null, false, List.of(), List.of(), null));
    }

    @Test
    void ac1Ac2GarantiesAvecLeurPrixLePrenomEtSansAvenantEnAttente() {
        when(avenants.findFirstByContratIdAndStatut(3L, "EN_ATTENTE")).thenReturn(Optional.empty());

        GarantiesContratResponse reponse = service.garanties(NUMERO);

        assertThat(reponse.prenom()).isEqualTo("Camille");
        assertThat(reponse.cotisationMensuelle()).isEqualByComparingTo("47.50");
        assertThat(reponse.garanties()).extracting(GarantiesContratResponse.Garantie::code)
                .containsExactly("RC", "PROTECTION_JURIDIQUE", "ASSISTANCE_0KM");
        assertThat(reponse.garanties().getFirst()).satisfies(rc -> {
            assertThat(rc.modifiable()).isFalse();
            assertThat(rc.raison()).contains("obligatoire");
        });
        assertThat(reponse.avenantEnAttente()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"IMPAYE", "SUSPENDU", "RESILIATION_EN_COURS", "RESILIE", "RENONCE",
        "EN_ATTENTE_DE_SIGNATURE", "EN_ATTENTE_EFFET", "ACTIF_JUSTIFICATIFS_ATTENDUS"})
    void ac6LeServeurRefuseUnContratQuiNEstPasActif(String statut) {
        contrat.setStatut(statut);

        assertThatThrownBy(() -> service.garanties(NUMERO)).isInstanceOf(ModificationGarantiesImpossibleException.class);
        assertThatThrownBy(() -> service.preparer(NUMERO, new PreparationAvenantRequest(List.of("RC"))))
                .isInstanceOf(ModificationGarantiesImpossibleException.class);
        verifyNoInteractions(catalogue);
        verify(avenants, never()).saveAndFlush(any());
    }

    @Test
    void ac3PrepareLAvenantEnAttenteEtLeContratPasseAAvenantEnAttente() {
        when(avenants.enAttenteVerrouille(3L)).thenReturn(Optional.empty());

        ProjetAvenantResponse projet = service.preparer(NUMERO,
                new PreparationAvenantRequest(List.of("RC", "ASSISTANCE_0KM")));

        ArgumentCaptor<Avenant> enregistre = ArgumentCaptor.forClass(Avenant.class);
        verify(avenants).saveAndFlush(enregistre.capture());
        assertThat(enregistre.getValue()).satisfies(avenant -> {
            assertThat(avenant.getStatut()).isEqualTo("EN_ATTENTE");
            assertThat(avenant.getNumero()).isEqualTo(1);
            assertThat(avenant.getPrimeAvantTtc()).isEqualByComparingTo("47.50");
            assertThat(avenant.getPrimeApresTtc()).isEqualByComparingTo("48.50");
            // Date d'effet : lendemain (recalculée à l'acceptation).
            assertThat(avenant.getDateEffet()).isEqualTo(LocalDate.of(2026, 11, 27));
        });
        verify(lignes).saveAll(anyList());
        assertThat(projet.avenant().ajouts()).extracting(AvenantResponse.Garantie::code)
                .containsExactly("ASSISTANCE_0KM");
        assertThat(projet.avenant().retraits()).extracting(AvenantResponse.Garantie::code)
                .containsExactly("PROTECTION_JURIDIQUE");
        // AC5 : le contrat affiche « Avenant en attente », les garanties et la cotisation ne changent pas encore.
        assertThat(contrat.getStatut()).isEqualTo("AVENANT_EN_ATTENTE");
        assertThat(contrat.getCotisationMensuelle()).isEqualByComparingTo("47.50");
        ArgumentCaptor<HistoriqueStatutContrat> trace = ArgumentCaptor.forClass(HistoriqueStatutContrat.class);
        verify(historique).save(trace.capture());
        assertThat(trace.getValue().getStatutAvant()).isEqualTo("ACTIF");
        assertThat(trace.getValue().getStatutApres()).isEqualTo("AVENANT_EN_ATTENTE");
        assertThat(trace.getValue().getAuteur()).isEqualTo("CLIENT");
    }

    @Test
    void unNouvelAvenantPrepareRemplaceLAncienEnAttenteQuiPasseAAbandonne() {
        contrat.setStatut("AVENANT_EN_ATTENTE");
        Avenant ancien = HistoriqueAvenantsTest.avenant(4L, 1, "EN_ATTENTE", AUJOURDHUI, "47.50", "51.00");
        ancien.setDocumentId(30L);
        Document projet = new Document();
        when(avenants.enAttenteVerrouille(3L)).thenReturn(Optional.of(ancien));
        when(documents.findById(30L)).thenReturn(Optional.of(projet));
        when(avenants.prochainNumero(3L)).thenReturn(2);

        service.preparer(NUMERO, new PreparationAvenantRequest(List.of("RC", "PROTECTION_JURIDIQUE",
                "ASSISTANCE_0KM")));

        // L'ancien est abandonné et écrit avant le nouveau (un seul EN_ATTENTE par contrat) ; rien n'est effacé.
        InOrder ordre = inOrder(avenants);
        ordre.verify(avenants).saveAndFlush(ancien);
        ordre.verify(avenants).saveAndFlush(org.mockito.ArgumentMatchers.argThat(avenant -> avenant.getNumero() == 2));
        assertThat(ancien.getStatut()).isEqualTo("ABANDONNE");
        assertThat(projet.getStatut()).isEqualTo("REMPLACE");
        verify(avenants, never()).delete(any());
        // Déjà « Avenant en attente » : pas de nouvelle ligne d'historique.
        verify(historique, never()).save(any());
    }

    @Test
    void continuerSansChangementEstRefuse() {
        assertThatThrownBy(() -> service.preparer(NUMERO,
                new PreparationAvenantRequest(List.of("RC", "PROTECTION_JURIDIQUE"))))
                .isInstanceOf(AvenantInvalideException.class)
                .hasMessage(AvenantInvalideException.AUCUN_CHANGEMENT);
        verify(avenants, never()).saveAndFlush(any());
    }

    @Test
    void ac5AbandonnerPasseLAvenantAAbandonneEtLeContratRedevientActif() {
        contrat.setStatut("AVENANT_EN_ATTENTE");
        Avenant enAttente = HistoriqueAvenantsTest.avenant(4L, 1, "EN_ATTENTE", AUJOURDHUI, "47.50", "51.00");
        when(avenants.enAttenteVerrouille(3L)).thenReturn(Optional.of(enAttente));
        when(lignes.findByAvenantIdOrderById(4L)).thenReturn(List.of());

        AvenantResponse reponse = service.abandonner(NUMERO);

        assertThat(reponse.statut()).isEqualTo("ABANDONNE");
        assertThat(enAttente.getStatut()).isEqualTo("ABANDONNE");
        assertThat(contrat.getStatut()).isEqualTo("ACTIF");
        verify(avenants, never()).delete(any());
    }

    @Test
    void abandonnerSansAvenantEnAttenteRepond404() {
        when(avenants.enAttenteVerrouille(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.abandonner(NUMERO)).isInstanceOf(AvenantInconnuException.class);
    }

    @Test
    void unContratDevenuImpayeGardeSonStatutALAbandon() {
        contrat.setStatut("IMPAYE");

        service.revenirAActif(contrat, "test");

        assertThat(contrat.getStatut()).isEqualTo("IMPAYE");
        verify(historique, never()).save(any());
    }
}
