package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.ContratResponse;
import net.checkconsulting.assuranceiard.api.dto.GenerationContratRequest;
import net.checkconsulting.assuranceiard.api.dto.SignatureContratRequest;
import net.checkconsulting.assuranceiard.api.exception.CodeSignatureIncorrectException;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.exception.MandatNonSigneException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.model.MandatSepa;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DossierSouscriptionRepository;
import net.checkconsulting.assuranceiard.api.repository.MandatSepaRepository;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContratServiceTest {

    private static final String JETON = "Bearer jeton";
    private static final UUID REFERENCE = UUID.randomUUID();
    private static final Instant MAINTENANT = Instant.parse("2026-11-05T10:00:00Z");

    private final SessionService sessionService = mock(SessionService.class);
    private final DossierSouscriptionRepository dossiers = mock(DossierSouscriptionRepository.class);
    private final ContratRepository contrats = mock(ContratRepository.class);
    private final MandatSepaRepository mandats = mock(MandatSepaRepository.class);
    private final RecommandationDevisRepository recommandations = mock(RecommandationDevisRepository.class);
    private final ContratService service = new ContratService(
            sessionService, dossiers, contrats, mandats, recommandations,
            Clock.fixed(MAINTENANT, ZoneOffset.UTC));

    private final Compte compte = new Compte();
    private final DossierSouscription dossier = new DossierSouscription();
    private final MandatSepa mandat = new MandatSepa();
    private final RecommandationDevis recommandation = new RecommandationDevis();

    @BeforeEach
    void setUp() {
        compte.setCivilite("M.");
        compte.setPrenom("Jean");
        compte.setNom("Dupont");
        compte.setEmail("jean.dupont@example.com");
        compte.setTelephone("0612345678");
        dossier.setCompte(compte);
        dossier.setReferenceDevis(REFERENCE);
        mandat.setDossier(dossier);
        mandat.setRum("AE-2026-ABCDEF12");
        mandat.setIbanFin("7890");
        mandat.setIbanDebut("FR76");
        mandat.setJourPrelevement(10);
        mandat.setDateDebut(LocalDate.of(2026, 12, 1));
        mandat.setDatePremierPrelevement(LocalDate.of(2026, 12, 10));
        mandat.setDateSignature(MAINTENANT);
        recommandation.setFormule("confort");
        recommandation.setOptions(List.of("assistance-depannage"));

        when(sessionService.compteConnecte(JETON)).thenReturn(compte);
        when(dossiers.findByCompteAndReferenceDevis(compte, REFERENCE)).thenReturn(Optional.of(dossier));
        when(recommandations.findByReference(REFERENCE)).thenReturn(Optional.of(recommandation));
        when(mandats.findByDossier(dossier)).thenReturn(Optional.of(mandat));
        when(contrats.findByDossier(dossier)).thenReturn(Optional.empty());
        when(contrats.prochainNumeroAnnee("AE-2026")).thenReturn(1L);
        when(contrats.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void genereUnContratAvecNumeroFormate() {
        ContratResponse reponse = service.generer(JETON, uneDemandeValide());

        assertThat(reponse.numero()).isEqualTo("AE-2026-000001");
        assertThat(reponse.statut()).isEqualTo("EN_ATTENTE_DE_SIGNATURE");
    }

    @Test
    void calculeLesCotisationsCorrectement() {
        ContratResponse reponse = service.generer(JETON, uneDemandeValide());

        assertThat(reponse.cotisationMensuelle()).isEqualByComparingTo("18.50");
        assertThat(reponse.cotisationAnnuelle()).isEqualByComparingTo("222.00");
    }

    @Test
    void utiliseLaDateDebutDuMandatCommeDateEffet() {
        ContratResponse reponse = service.generer(JETON, uneDemandeValide());

        assertThat(reponse.dateEffet()).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void inclutLesInformationsDuSouscripteur() {
        ContratResponse reponse = service.generer(JETON, uneDemandeValide());

        assertThat(reponse.prenom()).isEqualTo("Jean");
        assertThat(reponse.nom()).isEqualTo("Dupont");
        assertThat(reponse.telephone()).isEqualTo("0612345678");
        assertThat(reponse.formule()).isEqualTo("confort");
        assertThat(reponse.ibanFin()).isEqualTo("7890");
        assertThat(reponse.ibanDebut()).isEqualTo("FR76");
        assertThat(reponse.rum()).isEqualTo("AE-2026-ABCDEF12");
    }

    @Test
    void retourneLeContratExistantSansEnCreerUnDeuxieme() {
        Contrat existant = unContratExistant();
        when(contrats.findByDossier(dossier)).thenReturn(Optional.of(existant));

        ContratResponse reponse = service.generer(JETON, uneDemandeValide());

        assertThat(reponse.numero()).isEqualTo("AE-2026-000001");
        verify(contrats, never()).save(any());
    }

    @Test
    void lanceMandatNonSigneSiMandatSansDateSignature() {
        mandat.setDateSignature(null);

        assertThatThrownBy(() -> service.generer(JETON, uneDemandeValide()))
                .isInstanceOf(MandatNonSigneException.class);
    }

    @Test
    void lanceDevisInconnuSiDossierAbsent() {
        when(dossiers.findByCompteAndReferenceDevis(compte, REFERENCE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generer(JETON, uneDemandeValide()))
                .isInstanceOf(DevisInconnuException.class);
    }

    @Test
    void signaleResiliationHamonSiDemande() {
        ContratResponse reponse = service.generer(JETON, new GenerationContratRequest(
                REFERENCE, "12 rue des Lilas", "75001", "Paris",
                "appartement", 45, 300, new BigDecimal("18.50"), true));

        assertThat(reponse.resiliationHamon()).isTrue();
    }

    @Test
    void signeLeContratAvecLeCodeDevCorrect() {
        Contrat existant = unContratExistant();
        when(contrats.findByDossier(dossier)).thenReturn(Optional.of(existant));

        ContratResponse reponse = service.signer(JETON, REFERENCE, new SignatureContratRequest("123456"));

        assertThat(reponse.statut()).isEqualTo("SIGNE");
    }

    @Test
    void lanceCodeSignatureIncorrectSiCodeErrone() {
        Contrat existant = unContratExistant();
        when(contrats.findByDossier(dossier)).thenReturn(Optional.of(existant));

        assertThatThrownBy(() -> service.signer(JETON, REFERENCE, new SignatureContratRequest("999999")))
                .isInstanceOf(CodeSignatureIncorrectException.class);
    }

    private GenerationContratRequest uneDemandeValide() {
        return new GenerationContratRequest(
                REFERENCE, "12 rue des Lilas", "75001", "Paris",
                "appartement", 45, 300, new BigDecimal("18.50"), false);
    }

    private Contrat unContratExistant() {
        Contrat contrat = new Contrat();
        contrat.setNumero("AE-2026-000001");
        contrat.setStatut("EN_ATTENTE_DE_SIGNATURE");
        contrat.setDossier(dossier);
        contrat.setAdresseLogement("12 rue des Lilas");
        contrat.setCodePostal("75001");
        contrat.setVille("Paris");
        contrat.setTypeLogement("appartement");
        contrat.setSurface(45);
        contrat.setFranchise(300);
        contrat.setCotisationMensuelle(new BigDecimal("18.50"));
        contrat.setCotisationAnnuelle(new BigDecimal("222.00"));
        contrat.setResiliationHamon(false);
        contrat.setDateEffet(LocalDate.of(2026, 12, 1));
        contrat.setDateCreation(MAINTENANT);
        return contrat;
    }
}
