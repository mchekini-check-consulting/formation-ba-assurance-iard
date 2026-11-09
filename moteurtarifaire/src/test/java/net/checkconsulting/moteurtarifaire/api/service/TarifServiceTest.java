package net.checkconsulting.moteurtarifaire.api.service;

import static net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees.GRILLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees;
import net.checkconsulting.moteurtarifaire.api.dto.TarifHabitationRequest;
import net.checkconsulting.moteurtarifaire.api.dto.TarifHabitationResponse;
import net.checkconsulting.moteurtarifaire.api.exception.DonneeInvalideException;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleIndisponibleException;
import net.checkconsulting.moteurtarifaire.api.exception.TarifExpireException;
import net.checkconsulting.moteurtarifaire.api.exception.TarifInconnuException;
import net.checkconsulting.moteurtarifaire.api.mapper.TarifMapperImpl;
import net.checkconsulting.moteurtarifaire.api.enums.Formule;
import net.checkconsulting.moteurtarifaire.api.enums.OptionTarifaire;
import net.checkconsulting.moteurtarifaire.api.enums.Orientation;
import net.checkconsulting.moteurtarifaire.api.enums.ResiliationAnterieure;
import net.checkconsulting.moteurtarifaire.api.enums.StatutOccupant;
import net.checkconsulting.moteurtarifaire.api.service.recommandation.RegleConfort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TarifServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2030-06-15T10:00:00Z");
    private static final LocalDate AUJOURDHUI = LocalDate.ofInstant(MAINTENANT, ZoneOffset.UTC);
    private static final UUID ID_TARIF = UUID.randomUUID();

    @Mock
    private GrilleTarifaireService grilles;

    @Mock
    private TraceCalculService trace;

    private TarifService service;

    @BeforeEach
    void preparer() {
        service = new TarifService(
                grilles,
                new EligibiliteService(),
                new CalculateurPrix(new CalculateurCoefficient(JeuxDeDonnees.criteres())),
                new RecommandationService(List.of(new RegleConfort())),
                trace,
                new TarifMapperImpl(),
                Clock.fixed(MAINTENANT, ZoneOffset.UTC));
        lenient().when(grilles.applicableAu(AUJOURDHUI)).thenReturn(Optional.of(GRILLE));
    }

    @Test
    void ca1DonneLesPrixLaVersionEtUnIdentifiant() {
        TarifHabitationResponse reponse = service.calculer(JeuxDeDonnees.demandeCa1());

        assertThat(reponse.tarifable()).isTrue();
        assertThat(reponse.idTarif()).isNotNull();
        assertThat(reponse.versionGrille()).isEqualTo(GRILLE.version());
        assertThat(reponse.prix()).hasSize(Formule.values().length);
        assertThat(reponse.detail()).isNull();
        assertThat(reponse.formuleConseillee()).isNull();
    }

    @Test
    void ca2AvecUneFormuleLaReponseDonneLeDetail() {
        TarifHabitationRequest demande = JeuxDeDonnees.demandeCa1().toBuilder()
                .formule(Formule.CONFORT)
                .options(List.of(OptionTarifaire.OBJETS_VALEUR))
                .franchise(300)
                .build();

        assertThat(service.calculer(demande).detail().totalAnnuel()).isEqualByComparingTo("173.88");
    }

    @Test
    void uneFormuleSansOptionNiFranchiseDonneLePrixDeLaFormule() {
        TarifHabitationRequest demande = JeuxDeDonnees.demandeCa1().toBuilder().formule(Formule.CONFORT).build();

        TarifHabitationResponse reponse = service.calculer(demande);

        assertThat(reponse.detail().options()).isEmpty();
        assertThat(reponse.detail().totalMensuel()).isEqualByComparingTo(reponse.detail().prixFormule());
    }

    @Test
    void laDateDeValiditeDependDeLaGrille() {
        assertThat(service.calculer(JeuxDeDonnees.demandeCa1()).dateValidite())
                .isEqualTo(AUJOURDHUI.plusDays(GRILLE.dureeValiditeJours()));
    }

    @Test
    void ca3UnProprietaireNonOccupantNObtientAucunPrix() {
        TarifHabitationRequest demande = JeuxDeDonnees.demandeCa1().toBuilder()
                .statut(StatutOccupant.PROPRIETAIRE_NON_OCCUPANT)
                .formule(Formule.CONFORT)
                .besoins(JeuxDeDonnees.besoinsRequestConfort())
                .build();

        verifierRefus(service.calculer(demande), GRILLE.refus().statuts().get(StatutOccupant.PROPRIETAIRE_NON_OCCUPANT));
    }

    @Test
    void ca3UneResiliationPourNonPaiementNObtientAucunPrix() {
        TarifHabitationRequest demande = JeuxDeDonnees.demandeCa1().toBuilder()
                .resiliationAnterieure(ResiliationAnterieure.NON_PAIEMENT)
                .build();

        verifierRefus(service.calculer(demande), GRILLE.refus().resiliations().get(ResiliationAnterieure.NON_PAIEMENT));
    }

    private void verifierRefus(TarifHabitationResponse reponse, String motif) {
        assertThat(reponse.tarifable()).isFalse();
        assertThat(reponse.motif()).isEqualTo(motif);
        assertThat(reponse.orientation()).isEqualTo(Orientation.CONSEILLER);
        assertThat(reponse.prix()).isNull();
        assertThat(reponse.detail()).isNull();
        assertThat(reponse.formuleConseillee()).isNull();
        assertThat(reponse.dateValidite()).isNull();
    }

    @Test
    void avecDesBesoinsLaReponseDonneLaFormuleConseillee() {
        TarifHabitationRequest demande = JeuxDeDonnees.demandeCa1().toBuilder()
                .besoins(JeuxDeDonnees.besoinsRequestConfort())
                .build();

        TarifHabitationResponse reponse = service.calculer(demande);

        assertThat(reponse.formuleConseillee()).isEqualTo(Formule.CONFORT);
        assertThat(reponse.raisons()).isNotEmpty();
        assertThat(reponse.optionsConseillees()).isEmpty();
    }

    @Test
    void ca7ChaqueCalculEstTrace() {
        TarifHabitationRequest demande = JeuxDeDonnees.demandeCa1();

        TarifHabitationResponse reponse = service.calculer(demande);

        verify(trace).enregistrer(demande, reponse, MAINTENANT);
    }

    @Test
    void ca7UnRefusEstAussiTrace() {
        TarifHabitationRequest demande = JeuxDeDonnees.demandeCa1().toBuilder()
                .statut(StatutOccupant.PROPRIETAIRE_NON_OCCUPANT)
                .build();

        TarifHabitationResponse reponse = service.calculer(demande);

        verify(trace).enregistrer(demande, reponse, MAINTENANT);
    }

    @Test
    void uneFranchiseAbsenteDeLaGrilleEstRefuseeSansRienEnregistrer() {
        int franchiseInconnue = GRILLE.ajustementsFranchise().keySet().stream().reduce(0, Integer::sum) + 1;
        TarifHabitationRequest demande = JeuxDeDonnees.demandeCa1().toBuilder().franchise(franchiseInconnue).build();

        assertThatThrownBy(() -> service.calculer(demande))
                .isInstanceOfSatisfying(DonneeInvalideException.class,
                        erreur -> assertThat(erreur.getChamp()).isEqualTo("franchise"));
        verify(trace, never()).enregistrer(any(), any(), any());
    }

    @Test
    void sansGrilleEnVigueurLeCalculEstImpossible() {
        when(grilles.applicableAu(AUJOURDHUI)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculer(JeuxDeDonnees.demandeCa1()))
                .isInstanceOf(GrilleIndisponibleException.class);
        verify(trace, never()).enregistrer(any(), any(), any());
    }

    @Test
    void ca7UneRepriseRenvoieLeResultatDOrigineSansRecalcul() {
        TarifHabitationResponse origine = tarifValableJusquAu(AUJOURDHUI.plusDays(1));
        when(trace.relire(ID_TARIF)).thenReturn(Optional.of(origine));

        assertThat(service.reprendre(ID_TARIF)).isSameAs(origine);
        verify(grilles, never()).applicableAu(any());
    }

    @Test
    void leDernierJourDeValiditeEstInclus() {
        TarifHabitationResponse origine = tarifValableJusquAu(AUJOURDHUI);
        when(trace.relire(ID_TARIF)).thenReturn(Optional.of(origine));

        assertThat(service.reprendre(ID_TARIF)).isSameAs(origine);
    }

    @Test
    void leLendemainLeTarifEstExpire() {
        when(trace.relire(ID_TARIF)).thenReturn(Optional.of(tarifValableJusquAu(AUJOURDHUI.minusDays(1))));

        assertThatThrownBy(() -> service.reprendre(ID_TARIF)).isInstanceOf(TarifExpireException.class);
    }

    @Test
    void unRefusSansDateDeValiditeResteConsultable() {
        TarifHabitationResponse refus = tarifValableJusquAu(null);
        when(trace.relire(ID_TARIF)).thenReturn(Optional.of(refus));

        assertThat(service.reprendre(ID_TARIF)).isSameAs(refus);
    }

    @Test
    void unIdentifiantInconnuEstSignale() {
        when(trace.relire(ID_TARIF)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reprendre(ID_TARIF)).isInstanceOf(TarifInconnuException.class);
    }

    private TarifHabitationResponse tarifValableJusquAu(LocalDate date) {
        return TarifHabitationResponse.builder().idTarif(ID_TARIF).dateValidite(date).build();
    }

    @Test
    void unCodePostalHorsDesDepartementsDeLaGrilleEstRefuseSansRienEnregistrer() {
        TarifHabitationRequest demande = JeuxDeDonnees.demandeCa1().toBuilder().codePostal("00000").build();

        assertThatThrownBy(() -> service.calculer(demande))
                .isInstanceOfSatisfying(DonneeInvalideException.class,
                        erreur -> assertThat(erreur.getChamp()).isEqualTo("codePostal"));
        verify(trace, never()).enregistrer(any(), any(), any());
    }
}
