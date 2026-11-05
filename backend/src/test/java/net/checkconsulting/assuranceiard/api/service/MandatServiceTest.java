package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.MandatPreparationResponse;
import net.checkconsulting.assuranceiard.api.dto.MandatSignatureRequest;
import net.checkconsulting.assuranceiard.api.dto.MandatSigneResponse;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.exception.InformationRefuseeException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.model.MandatSepa;
import net.checkconsulting.assuranceiard.api.repository.DossierSouscriptionRepository;
import net.checkconsulting.assuranceiard.api.repository.MandatSepaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MandatServiceTest {

    private static final String JETON = "Bearer jeton";
    private static final String IBAN = "FR76 3000 6000 0112 3456 7890 189";
    private static final UUID REFERENCE = UUID.randomUUID();
    /** 5 novembre 2026, 23 h 30 UTC : déjà le 6 novembre en France. */
    private static final Instant MAINTENANT = Instant.parse("2026-11-05T23:30:00Z");
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 6);

    private final SessionService sessionService = mock(SessionService.class);
    private final DossierSouscriptionRepository dossiers = mock(DossierSouscriptionRepository.class);
    private final MandatSepaRepository mandats = mock(MandatSepaRepository.class);
    private final VerificationBancaire banque = mock(VerificationBancaire.class);
    private final MandatService service = new MandatService(
            sessionService,
            dossiers,
            mandats,
            new IbanService(),
            new PremierPrelevementService(),
            banque,
            Clock.fixed(MAINTENANT, ZoneOffset.UTC));

    private final Compte compte = new Compte();
    private final DossierSouscription dossier = new DossierSouscription();

    @BeforeEach
    void preparer() {
        when(sessionService.compteConnecte(JETON)).thenReturn(compte);
        when(dossiers.findByCompteAndReferenceDevis(compte, REFERENCE)).thenReturn(Optional.of(dossier));
        when(mandats.findByDossier(dossier)).thenReturn(Optional.empty());
        when(mandats.save(any())).thenAnswer(appel -> appel.getArgument(0));
        when(banque.compteValide(anyString())).thenReturn(true);
    }

    private MandatSignatureRequest demande(LocalDate debut, int jour, String iban) {
        return new MandatSignatureRequest(REFERENCE, " Marie Durand ", iban, debut, jour, true);
    }

    private MandatSignatureRequest demande(LocalDate debut) {
        return demande(debut, 10, IBAN);
    }

    private void refuse(MandatSignatureRequest demande, String champ) {
        assertThatThrownBy(() -> service.signer(JETON, demande))
                .isInstanceOfSatisfying(InformationRefuseeException.class, erreur ->
                        assertThat(erreur.getChamp()).isEqualTo(champ));
    }

    @Test
    void preparerAttribueUneRumEtIndiqueLeCreancier() {
        MandatPreparationResponse reponse = service.preparer(JETON, REFERENCE);

        assertThat(reponse.creancier()).isEqualTo("Assur'Easy");
        assertThat(reponse.ics()).isEqualTo("FR00ZZZ000000");
        assertThat(reponse.rum()).matches("AE-2026-[A-HJ-NP-Z2-9]{8}");
    }

    @Test
    void chaqueMandatRecoitSaPropreRum() {
        String premiere = service.preparer(JETON, REFERENCE).rum();
        String seconde = service.preparer(JETON, REFERENCE).rum();

        assertThat(premiere).isNotEqualTo(seconde);
    }

    @Test
    void unMemeDossierGardeSaRum() {
        MandatSepa existant = new MandatSepa();
        existant.setRum("AE-2026-EXISTANT");
        when(mandats.findByDossier(dossier)).thenReturn(Optional.of(existant));

        assertThat(service.preparer(JETON, REFERENCE).rum()).isEqualTo("AE-2026-EXISTANT");
        verify(mandats, never()).save(any());
    }

    @Test
    void refuseUnDossierQuiNestPasCeluiDuClient() {
        UUID autre = UUID.randomUUID();
        when(dossiers.findByCompteAndReferenceDevis(compte, autre)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.preparer(JETON, autre)).isInstanceOf(DevisInconnuException.class);
    }

    @Test
    void signerEnregistreLeMandatSansLIbanComplet() {
        MandatSepa mandat = new MandatSepa();
        mandat.setRum("AE-2026-EXISTANT");
        when(mandats.findByDossier(dossier)).thenReturn(Optional.of(mandat));

        MandatSigneResponse reponse = service.signer(JETON, demande(AUJOURDHUI.plusDays(1)));

        assertThat(reponse.rum()).isEqualTo("AE-2026-EXISTANT");
        assertThat(reponse.datePremierPrelevement()).isEqualTo(LocalDate.of(2026, 12, 10));
        assertThat(mandat.getIbanFin()).isEqualTo("0189");
        assertThat(mandat.getTitulaire()).isEqualTo("Marie Durand");
        assertThat(mandat.getJourPrelevement()).isEqualTo(10);
        assertThat(mandat.getDateDebut()).isEqualTo(AUJOURDHUI.plusDays(1));
        assertThat(mandat.getDatePremierPrelevement()).isEqualTo(LocalDate.of(2026, 12, 10));
        assertThat(mandat.getDateSignature()).isEqualTo(MAINTENANT);
        verify(banque).compteValide("FR7630006000011234567890189");
    }

    @Test
    void signerSansPreparationCreeLeMandat() {
        assertThat(service.signer(JETON, demande(AUJOURDHUI.plusDays(1))).rum()).startsWith("AE-2026-");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 90})
    void rg1AccepteDuLendemainA90Jours(int delai) {
        assertThatCode(() -> service.signer(JETON, demande(AUJOURDHUI.plusDays(delai))))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"-1, plus tôt", "0, plus tôt", "91, 90 prochains jours"})
    void rg1RefuseLesAutresDates(int delai, String message) {
        LocalDate debut = AUJOURDHUI.plusDays(delai);

        assertThatThrownBy(() -> service.signer(JETON, demande(debut)))
                .isInstanceOfSatisfying(InformationRefuseeException.class, erreur -> {
                    assertThat(erreur.getChamp()).isEqualTo("dateDebut");
                    assertThat(erreur.getMessage()).contains(message);
                });
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 15})
    void rg3AccepteLes3Jours(int jour) {
        assertThatCode(() -> service.signer(JETON, demande(AUJOURDHUI.plusDays(1), jour, IBAN)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 20, 31})
    void rg3RefuseLesAutresJours(int jour) {
        refuse(demande(AUJOURDHUI.plusDays(1), jour, IBAN), "jourPrelevement");
    }

    @Test
    void rg2RefuseUnIbanFauxSansInterrogerLaBanque() {
        refuse(demande(AUJOURDHUI.plusDays(1), 10, "FR7630006000011234567890188"), "iban");

        verify(banque, never()).compteValide(anyString());
    }

    @Test
    void rg2RefuseUnCompteRejeteParLaBanque() {
        when(banque.compteValide(anyString())).thenReturn(false);

        refuse(demande(AUJOURDHUI.plusDays(1)), "iban");
    }

    @Test
    void leBouchonAccepteTousLesComptes() {
        assertThat(new VerificationBancaireBouchon().compteValide("FR7630006000011234567890189")).isTrue();
    }

    @Test
    void leTexteTechniqueDeLaDemandeNeContientPasLIban() {
        assertThat(demande(AUJOURDHUI).toString()).doesNotContain("3000", "0189");
    }
}
