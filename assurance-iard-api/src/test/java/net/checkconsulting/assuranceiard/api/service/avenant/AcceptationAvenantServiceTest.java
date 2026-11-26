package net.checkconsulting.assuranceiard.api.service.avenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.AcceptationAvenantRequest;
import net.checkconsulting.assuranceiard.api.dto.AvenantAccepteResponse;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.dto.RegenerationResponse;
import net.checkconsulting.assuranceiard.api.exception.AvenantInconnuException;
import net.checkconsulting.assuranceiard.api.exception.AvenantInvalideException;
import net.checkconsulting.assuranceiard.api.model.Avenant;
import net.checkconsulting.assuranceiard.api.model.AvenantLigne;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.ContratOption;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.AvenantLigneRepository;
import net.checkconsulting.assuranceiard.api.repository.AvenantRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratOptionRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.service.DocumentService;
import net.checkconsulting.assuranceiard.api.service.EnvoiAvenant;
import net.checkconsulting.assuranceiard.api.service.document.DemandeDocument;
import net.checkconsulting.assuranceiard.api.service.document.DocumentsGeneresService;
import net.checkconsulting.assuranceiard.api.service.document.MotifRegeneration;
import net.checkconsulting.assuranceiard.api.service.document.RegenerationContratService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.mail.MailSendException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionOperations;

/** US-23, AC4 et AC7 : accepter l'avenant (horodaté, conservé), puis conditions particulières et e-mail. */
class AcceptationAvenantServiceTest {

    private static final String NUMERO = "AE-RECETTE-A03";
    /** HistoriqueAvenantsTest.HORLOGE : 26/11/2026 à 23h30 à Paris. */
    private static final LocalDate EFFET = LocalDate.of(2026, 11, 27);

    private final AvenantService avenantService = mock(AvenantService.class);
    private final ContratRepository contrats = mock(ContratRepository.class);
    private final AvenantRepository avenants = mock(AvenantRepository.class);
    private final AvenantLigneRepository lignes = mock(AvenantLigneRepository.class);
    private final ContratOptionRepository options = mock(ContratOptionRepository.class);
    private final CatalogueGaranties catalogue = mock(CatalogueGaranties.class);
    private final DocumentsGeneresService documentsGeneres = mock(DocumentsGeneresService.class);
    private final RegenerationContratService regeneration = mock(RegenerationContratService.class);
    private final EnvoiAvenant envoi = mock(EnvoiAvenant.class);
    private final AcceptationAvenantService service = new AcceptationAvenantService(avenantService, contrats,
            avenants, lignes, options, catalogue, new HistoriqueAvenants(avenants, lignes, HistoriqueAvenantsTest.HORLOGE),
            documentsGeneres, regeneration, envoi, TransactionOperations.withoutTransaction(),
            HistoriqueAvenantsTest.HORLOGE);

    private Contrat contrat;
    private Avenant avenant;
    private ContratOption protectionJuridique;
    private final List<ContratOption> enregistrees = new ArrayList<>();
    private final Document pdf = new Document();

    @BeforeEach
    void preparer() {
        contrat = CatalogueGarantiesTest.contrat(Contrat.AUTO, "TR");
        contrat.setStatut("AVENANT_EN_ATTENTE");
        contrat.setCotisationMensuelle(new BigDecimal("47.50"));
        contrat.setCotisationAnnuelle(new BigDecimal("570.00"));
        contrat.setDateEffet(LocalDate.of(2026, 9, 27));
        Compte compte = new Compte();
        compte.setEmail("recette.auto@assureasy.fr");
        compte.setPrenom("Camille");
        ReflectionTestUtils.setField(contrat, "compte", compte);
        avenant = HistoriqueAvenantsTest.avenant(9L, 1, "EN_ATTENTE", LocalDate.of(2026, 11, 20), "47.50", "48.50");
        protectionJuridique = new ContratOption();
        protectionJuridique.setCodeOption("PROTECTION_JURIDIQUE");
        DonneesContrat donnees = CatalogueGarantiesTest.donnees(Contrat.AUTO, List.of());

        when(avenantService.contratModifiable(NUMERO)).thenReturn(contrat);
        when(avenantService.donnees(contrat)).thenReturn(donnees);
        when(avenants.enAttenteVerrouille(3L)).thenReturn(Optional.of(avenant));
        when(lignes.findByAvenantIdOrderById(9L)).thenReturn(List.of(
                HistoriqueAvenantsTest.ligne(9L, "ASSISTANCE_0KM", CalculAvenant.AJOUT, "3.50"),
                HistoriqueAvenantsTest.ligne(9L, "PROTECTION_JURIDIQUE", CalculAvenant.RETRAIT, "2.50")));
        when(options.existsByContratId(3L)).thenReturn(true);
        when(options.enCours(3L, EFFET)).thenReturn(List.of(protectionJuridique));
        when(options.save(any(ContratOption.class))).thenAnswer(appel -> {
            enregistrees.add(appel.getArgument(0));
            return appel.getArgument(0);
        });
        pdf.setId(70L);
        pdf.setStocke(true);
        when(documentsGeneres.generer(any(DemandeDocument.class))).thenReturn(pdf);
        when(documentsGeneres.telecharger(70L)).thenReturn(
                new DocumentService.Fichier("avenant-AE-RECETTE-A03-v1.pdf", "pdf", new byte[] {'%', 'P'}));
        when(regeneration.regenerer(3L, MotifRegeneration.AVENANT))
                .thenReturn(new RegenerationResponse(80L, 2, Instant.EPOCH, true, true));
    }

    private AvenantAccepteResponse accepter() {
        return service.accepter(NUMERO, new AcceptationAvenantRequest(true), "192.0.2.10");
    }

    @Test
    void ac4LaCaseDoitEtreCocheeRg1() {
        assertThatThrownBy(() -> service.accepter(NUMERO, new AcceptationAvenantRequest(false), "192.0.2.10"))
                .isInstanceOf(AvenantInvalideException.class)
                .hasMessage(AvenantInvalideException.ACCEPTATION_REQUISE);
        assertThatThrownBy(() -> service.accepter(NUMERO, null, "192.0.2.10"))
                .isInstanceOf(AvenantInvalideException.class);
        verifyNoInteractions(avenants, documentsGeneres, regeneration, envoi);
    }

    @Test
    void ac4AcceptationHorodateeConserveeEffetLeLendemainNouvellesGarantiesEtCotisation() {
        AvenantAccepteResponse reponse = accepter();

        assertThat(avenant.getStatut()).isEqualTo("ACCEPTE");
        assertThat(avenant.getAccepteLe()).isEqualTo(Instant.parse("2026-11-26T22:30:00Z"));
        assertThat(avenant.getAccepteIp()).isEqualTo("192.0.2.10");
        // Date d'effet : le lendemain de l'acceptation (jamais la date de préparation).
        assertThat(avenant.getDateEffet()).isEqualTo(EFFET);
        assertThat(avenant.getDocumentId()).isEqualTo(70L);
        // Garantie ajoutée à la date d'effet ; garantie retirée la veille.
        assertThat(enregistrees).singleElement().satisfies(option -> {
            assertThat(option.getCodeOption()).isEqualTo("ASSISTANCE_0KM");
            assertThat(option.getPrixMensuelTtc()).isEqualByComparingTo("3.50");
            assertThat(option.getDateDebut()).isEqualTo(EFFET);
            assertThat(option.getObligatoire()).isFalse();
        });
        assertThat(protectionJuridique.getDateFin()).isEqualTo(LocalDate.of(2026, 11, 26));
        // Nouvelle cotisation : 48,50 € par mois, 582,00 € par an.
        assertThat(contrat.getCotisationMensuelle()).isEqualByComparingTo("48.50");
        assertThat(contrat.getCotisationAnnuelle()).isEqualByComparingTo("582.00");
        verify(avenantService).revenirAActif(contrat, "Avenant accepté en ligne");

        assertThat(reponse.avenant().numero()).isEqualTo("AV-2026-000009");
        assertThat(reponse.avenant().statut()).isEqualTo("ACCEPTE");
        assertThat(reponse.disponible()).isTrue();
        assertThat(reponse.emailEnvoye()).isTrue();
        assertThat(reponse.versionConditionsParticulieres()).isEqualTo(2);
    }

    @Test
    void ac4LePdfDeLAvenantAPourReferenceLeNumeroDAvenantEtPartParEmail() {
        accepter();

        ArgumentCaptor<DemandeDocument> demande = ArgumentCaptor.forClass(DemandeDocument.class);
        verify(documentsGeneres).generer(demande.capture());
        assertThat(demande.getValue().type()).isEqualTo(TypeDocument.AVENANT);
        assertThat(demande.getValue().reference()).isEqualTo("AV-2026-000009");
        assertThat(demande.getValue().contratId()).isEqualTo(3L);
        verify(envoi).envoyer(eq("recette.auto@assureasy.fr"), eq("Camille"), eq("AE-RECETTE-A03"),
                eq("AV-2026-000009"), eq("avenant-AE-RECETTE-A03-v1.pdf"), any());
    }

    @Test
    void ac7LesConditionsParticulieresSontRegenereesApresLEnregistrementCompletMotifAvenant() {
        accepter();

        InOrder ordre = inOrder(avenants, documentsGeneres, regeneration, envoi);
        ordre.verify(avenants).saveAndFlush(avenant);
        ordre.verify(documentsGeneres).generer(any());
        ordre.verify(avenants).save(avenant);
        ordre.verify(regeneration).regenerer(3L, MotifRegeneration.AVENANT);
        ordre.verify(envoi).envoyer(any(), any(), any(), any(), any(), any());
    }

    @Test
    void siLePdfNEstPasProduitLAcceptationResteEnregistreeEtLaRepriseSEnCharge() {
        pdf.setStocke(false);
        when(regeneration.regenerer(anyLong(), any())).thenThrow(new IllegalStateException("PDF"));

        AvenantAccepteResponse reponse = accepter();

        assertThat(avenant.getStatut()).isEqualTo("ACCEPTE");
        assertThat(reponse.disponible()).isFalse();
        assertThat(reponse.emailEnvoye()).isFalse();
        assertThat(reponse.versionConditionsParticulieres()).isNull();
        verifyNoInteractions(envoi);
    }

    @Test
    void unEchecDEnvoiNeRemetPasEnCauseLAcceptation() {
        org.mockito.Mockito.doThrow(new MailSendException("smtp")).when(envoi)
                .envoyer(any(), any(), any(), any(), any(), any());

        AvenantAccepteResponse reponse = accepter();

        assertThat(reponse.emailEnvoye()).isFalse();
        assertThat(avenant.getStatut()).isEqualTo("ACCEPTE");
    }

    @Test
    void aucunAvenantEnAttenteRepond404() {
        when(avenants.enAttenteVerrouille(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(this::accepter).isInstanceOf(AvenantInconnuException.class);
        verify(regeneration, never()).regenerer(anyLong(), any());
    }

    @Test
    void contratDuSprint1SesOptionsDuDevisSontEnregistreesAvantLAvenant() {
        DonneesContrat sprint1 = CatalogueGarantiesTest.donnees(Contrat.HABITATION,
                List.of(new DonneesContrat.Option("assistance-depannage", null)));
        when(avenantService.donnees(contrat)).thenReturn(sprint1);
        when(options.existsByContratId(3L)).thenReturn(false);
        when(catalogue.garanties(contrat, sprint1, contrat.getDateEffet())).thenReturn(List.of(
                new GarantieProposee("assistance-depannage", "Assistance dépannage", new BigDecimal("1.50"), true,
                        false, null),
                new GarantieProposee("objets-valeur", "Objets de valeur", new BigDecimal("2.50"), false, false,
                        null)));
        when(lignes.findByAvenantIdOrderById(9L)).thenReturn(List.<AvenantLigne>of(
                HistoriqueAvenantsTest.ligne(9L, "objets-valeur", CalculAvenant.AJOUT, "2.50")));

        accepter();

        assertThat(enregistrees).extracting(ContratOption::getCodeOption, ContratOption::getDateDebut)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("assistance-depannage", contrat.getDateEffet()),
                        org.assertj.core.groups.Tuple.tuple("objets-valeur", EFFET));
    }
}
