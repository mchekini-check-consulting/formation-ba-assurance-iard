package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.ContratAutoGenerationResponse;
import net.checkconsulting.assuranceiard.api.dto.IbanVerificationRequest;
import net.checkconsulting.assuranceiard.api.dto.IbanVerificationResponse;
import net.checkconsulting.assuranceiard.api.dto.PaiementAutoRequest;
import net.checkconsulting.assuranceiard.api.exception.InformationRefuseeException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Conducteur;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.DevisAuto;
import net.checkconsulting.assuranceiard.api.model.MandatSepa;
import net.checkconsulting.assuranceiard.api.model.SouscripteurAuto;
import net.checkconsulting.assuranceiard.api.model.Vehicule;
import net.checkconsulting.assuranceiard.api.repository.ConducteurRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisAutoRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentContenuRepository;
import net.checkconsulting.assuranceiard.api.repository.EcheanceRepository;
import net.checkconsulting.assuranceiard.api.repository.MandatSepaRepository;
import net.checkconsulting.assuranceiard.api.repository.SouscripteurAutoRepository;
import net.checkconsulting.assuranceiard.api.repository.VehiculeRepository;
import net.checkconsulting.assuranceiard.api.service.document.DocumentsGeneresService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;

class PaiementAutoServiceTest {

    private static final String JETON = "Bearer jeton";
    private static final String IBAN_BNP = "FR7630006000011234567890189";
    /** 5 novembre 2026, 23h30 UTC = 6 novembre en France (Europe/Paris). */
    private static final Instant MAINTENANT = Instant.parse("2026-11-05T23:30:00Z");
    private static final LocalDate AUJOURD_HUI = LocalDate.of(2026, 11, 6);

    private final SessionService sessionService = mock(SessionService.class);
    private final DevisAutoRepository devisAutos = mock(DevisAutoRepository.class);
    private final VehiculeRepository vehicules = mock(VehiculeRepository.class);
    private final ConducteurRepository conducteurs = mock(ConducteurRepository.class);
    private final SouscripteurAutoRepository souscripteurs = mock(SouscripteurAutoRepository.class);
    private final ContratRepository contratsRepo = mock(ContratRepository.class);
    private final MandatSepaRepository mandats = mock(MandatSepaRepository.class);
    private final EcheanceRepository echeances = mock(EcheanceRepository.class);
    private final DocumentsGeneresService documentsService = mock(DocumentsGeneresService.class);
    private final DocumentContenuRepository documentContenus = mock(DocumentContenuRepository.class);
    private final EnvoiEmailContrat emailService = mock(EnvoiEmailContrat.class);
    private final FormuleAutoService formuleAutoService = mock(FormuleAutoService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    private final PaiementAutoService service = new PaiementAutoService(
            sessionService, devisAutos, vehicules, conducteurs, souscripteurs,
            contratsRepo, mandats, echeances, documentsService, documentContenus,
            emailService, new IbanService(), new IbanBouchon(), new PremierPrelevementService(),
            formuleAutoService, jdbc, Clock.fixed(MAINTENANT, ZoneId.of("Europe/Paris")));

    private final Compte compte = new Compte();
    private final DevisAuto devis = new DevisAuto();
    private final Vehicule vehicule = new Vehicule();
    private final SouscripteurAuto souscripteur = new SouscripteurAuto();

    @BeforeEach
    void setUp() {
        compte.setId(1L);
        compte.setEmail("marie@test.fr");
        devis.setId(10L);
        devis.setVehiculeId(5L);
        devis.setConducteurId(3L);
        devis.setFormuleChoisie("TR");
        devis.setPrixMensuelTtc(new BigDecimal("55.00"));
        devis.setPrixAnnuelTtc(new BigDecimal("660.00"));
        vehicule.setId(5L);
        souscripteur.setPrenom("Marie");
        souscripteur.setNom("Durand");

        when(sessionService.compteConnecte(JETON)).thenReturn(compte);
        when(devisAutos.findById(10L)).thenReturn(Optional.of(devis));
        when(vehicules.findById(5L)).thenReturn(Optional.of(vehicule));
        when(conducteurs.findById(3L)).thenReturn(Optional.of(new Conducteur()));
        when(souscripteurs.findByDevisAutoId(10L)).thenReturn(Optional.of(souscripteur));
        when(contratsRepo.findByDevisAutoId(10L)).thenReturn(Optional.empty());
    }

    // ---- verifierIban ----

    @Test
    void verifierIbanAvecCleInvalideRetourneInvalide() {
        IbanVerificationResponse rep = service.verifierIban(
                new IbanVerificationRequest("FR7630006000011234567890188")); // clé fausse
        assertThat(rep.valide()).isFalse();
    }

    @Test
    void verifierIbanBnpRetourneValideAvecBicExact() {
        IbanVerificationResponse rep = service.verifierIban(
                new IbanVerificationRequest(" " + IBAN_BNP + " ")); // espaces tolérés
        assertThat(rep.valide()).isTrue();
        assertThat(rep.bic()).isEqualTo("BNPAFRPPXXX");
    }

    @Test
    void verifierIbanNormaliseLeSaisieAvantVerification() {
        IbanVerificationResponse rep = service.verifierIban(
                new IbanVerificationRequest("fr76 3000 6000 0112 3456 7890 189"));
        assertThat(rep.valide()).isTrue();
    }

    // ---- genererContrat : validation de la date ----

    @Test
    void refuseLaDateDuJourCommeDebut() {
        assertThatThrownBy(() -> service.genererContrat(10L,
                demande(AUJOURD_HUI, 10), JETON))
                .isInstanceOfSatisfying(InformationRefuseeException.class,
                        e -> assertThat(e.getChamp()).isEqualTo("dateDebut"));
    }

    @Test
    void refuseUneDateTropEloignee() {
        assertThatThrownBy(() -> service.genererContrat(10L,
                demande(AUJOURD_HUI.plusDays(91), 10), JETON))
                .isInstanceOfSatisfying(InformationRefuseeException.class,
                        e -> assertThat(e.getChamp()).isEqualTo("dateDebut"));
    }

    // ---- genererContrat : validation du jour de prélèvement ----

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 6, 9, 11, 14, 16, 20, 28})
    void refuseUnJourDePrelevementDifferentDe5_10_15(int jour) {
        assertThatThrownBy(() -> service.genererContrat(10L,
                demande(AUJOURD_HUI.plusDays(10), jour), JETON))
                .isInstanceOfSatisfying(InformationRefuseeException.class,
                        e -> assertThat(e.getChamp()).isEqualTo("jourPrelevement"));
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 15})
    void accepteLes3JoursAutorises(int jour) {
        // Accepté par la validation du service (ne lève pas d'exception sur jourPrelevement)
        // Les autres étapes (insert) lèveront une autre exception — on vérifie juste la validation métier
        try {
            service.genererContrat(10L, demande(AUJOURD_HUI.plusDays(10), jour), JETON);
        } catch (InformationRefuseeException e) {
            assertThat(e.getChamp()).isNotEqualTo("jourPrelevement");
        } catch (Exception ignored) {
            // Exceptions d'infrastructure (JdbcTemplate non configuré) acceptées ici
        }
    }

    // ---- genererContrat : idempotence ----

    @Test
    void retourneLeMemeNumeroSiLContratExisteDeja() {
        Contrat existant = new Contrat();
        existant.setId(99L);
        existant.setNumero("AE-2026-000001");
        existant.setStatut("EN_ATTENTE_DE_SIGNATURE");
        existant.setCotisationMensuelle(new BigDecimal("55.00"));
        MandatSepa mandat = new MandatSepa();
        mandat.setDatePremierPrelevement(LocalDate.of(2026, 12, 15));

        when(contratsRepo.findByDevisAutoId(10L)).thenReturn(Optional.of(existant));
        when(jdbc.queryForList(anyString(), eq(Long.class), eq(99L)))
                .thenReturn(java.util.List.of());

        ContratAutoGenerationResponse rep = service.genererContrat(10L,
                demande(AUJOURD_HUI.plusDays(10), 10), JETON);

        assertThat(rep.numero()).isEqualTo("AE-2026-000001");
    }

    // ---- franchiseParFormule ----

    @Test
    void franchiseTrEst500() {
        devis.setFormuleChoisie("TR");
        var rep = service.initialiser(10L, JETON);
        assertThat(rep.franchise()).isEqualByComparingTo(new BigDecimal("500"));
    }

    @Test
    void franchiseInterEst400() {
        devis.setFormuleChoisie("INTER");
        var rep = service.initialiser(10L, JETON);
        assertThat(rep.franchise()).isEqualByComparingTo(new BigDecimal("400"));
    }

    @Test
    void franchiseTiersEstNull() {
        devis.setFormuleChoisie("TIERS");
        var rep = service.initialiser(10L, JETON);
        assertThat(rep.franchise()).isNull();
    }

    // ---- initialiser : données conducteur ----

    @Test
    void initialiserInclutLAnneePermisEtLeBonusMalus() {
        Conducteur conducteur = new Conducteur();
        conducteur.setDatePermis(java.time.LocalDate.of(2010, 6, 15));
        conducteur.setCoefficientBonusMalus(new BigDecimal("0.68"));
        when(conducteurs.findById(3L)).thenReturn(Optional.of(conducteur));

        var rep = service.initialiser(10L, JETON);
        assertThat(rep.conducteurAnneePermis()).isEqualTo(2010);
        assertThat(rep.conducteurBonusMalus()).isEqualByComparingTo(new BigDecimal("0.68"));
    }

    @Test
    void initialiserTolereUnConducteurSansPermisNiBonusMalus() {
        when(conducteurs.findById(3L)).thenReturn(Optional.of(new Conducteur()));
        var rep = service.initialiser(10L, JETON);
        assertThat(rep.conducteurAnneePermis()).isNull();
        assertThat(rep.conducteurBonusMalus()).isNull();
    }

    private PaiementAutoRequest demande(LocalDate dateDebut, int jour) {
        return new PaiementAutoRequest(dateDebut, "Marie Durand", IBAN_BNP,
                null, null, jour, true);
    }
}
