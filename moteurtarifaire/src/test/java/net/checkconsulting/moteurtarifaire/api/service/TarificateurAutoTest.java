package net.checkconsulting.moteurtarifaire.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaireAuto;
import net.checkconsulting.moteurtarifaire.api.dto.TarifAutoRequest;
import net.checkconsulting.moteurtarifaire.api.dto.TarifAutoResponse;
import net.checkconsulting.moteurtarifaire.api.enums.FormuleAuto;
import net.checkconsulting.moteurtarifaire.api.enums.StationnementAuto;
import net.checkconsulting.moteurtarifaire.api.enums.UsageAuto;
import net.checkconsulting.moteurtarifaire.api.model.RefFormule;
import net.checkconsulting.moteurtarifaire.api.model.RefTarif;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TarificateurAutoTest {

    private static final LocalDate AUJOURD_HUI = LocalDate.of(2026, 6, 15);

    @Mock
    private GrilleTarifaireAutoService grilles;

    private TarificateurAuto service;

    @BeforeEach
    void preparer() {
        Clock horloge = Clock.fixed(AUJOURD_HUI.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new TarificateurAuto(grilles, horloge);
    }

    private GrilleTarifaireAuto grilleAvecCoeffsNeutres() {
        RefFormule tiers = formule("TIERS", "Tiers", new BigDecimal("25.00"), 1);
        RefFormule inter = formule("INTER", "Intermédiaire", new BigDecimal("35.00"), 2);
        RefFormule tr = formule("TR", "Tous risques", new BigDecimal("50.00"), 3);

        return new GrilleTarifaireAuto(
                "AUTO-2026.1",
                LocalDate.of(2026, 1, 1), null, 30,
                List.of(tiers, inter, tr),
                List.of(),
                coeffsNeutres());
    }

    private RefFormule formule(String code, String libelle, BigDecimal prixBase, int ordre) {
        RefFormule f = new RefFormule();
        f.setCode(code);
        f.setLibelle(libelle);
        f.setPrixMensuelBase(prixBase);
        f.setOrdre(ordre);
        return f;
    }

    private Map<String, List<RefTarif>> coeffsNeutres() {
        return Map.of(
                "AGE", List.of(tarif(null, null, null, null, BigDecimal.ONE)),
                "BONUS_MALUS", List.of(tarif(null, null, null, null, BigDecimal.ONE)),
                "USAGE", List.of(tarif(null, null, "PRIVE", null, BigDecimal.ONE),
                        tarif(null, null, "TRAJET_TRAVAIL", null, BigDecimal.ONE),
                        tarif(null, null, "PROFESSIONNEL", null, BigDecimal.ONE)),
                "VALEUR_VEHICULE", List.of(tarif(null, null, null, null, BigDecimal.ONE)),
                "KM_ANNUEL", List.of(tarif(null, null, null, null, BigDecimal.ONE)),
                "STATIONNEMENT", List.of(tarif(null, null, "GARAGE_FERME", null, BigDecimal.ONE),
                        tarif(null, null, "PARKING_PRIVE", null, BigDecimal.ONE),
                        tarif(null, null, "RUE", null, BigDecimal.ONE)),
                "SURPRIME_NOVICE", List.of(tarif(null, null, "OUI", null, new BigDecimal("1.30")),
                        tarif(null, null, "NON", null, BigDecimal.ONE)));
    }

    private RefTarif tarif(BigDecimal min, BigDecimal max, String valeur, BigDecimal coeff2, BigDecimal coeff) {
        RefTarif t = new RefTarif();
        t.setBorneMin(min);
        t.setBorneMax(max);
        t.setValeur(valeur);
        t.setCoefficient(coeff);
        return t;
    }

    private TarifAutoRequest demandeStandard() {
        return new TarifAutoRequest(
                2020, new BigDecimal("1.00"), 10000,
                UsageAuto.PRIVE, StationnementAuto.GARAGE_FERME,
                new BigDecimal("8000"), null, null);
    }

    @Test
    void calculNominalAvecCoeffsNeutres() {
        when(grilles.applicableAu(any())).thenReturn(Optional.of(grilleAvecCoeffsNeutres()));

        TarifAutoResponse reponse = service.calculer(demandeStandard());

        assertThat(reponse.tarifable()).isTrue();
        assertThat(reponse.versionGrille()).isEqualTo("AUTO-2026.1");
        assertThat(reponse.prix()).hasSize(3);
        assertThat(reponse.prix().get(0).formule()).isEqualTo("TIERS");
        assertThat(reponse.prix().get(0).mensuel()).isEqualByComparingTo("25.00");
        assertThat(reponse.prix().get(0).annuel()).isEqualByComparingTo("300.00");
    }

    @Test
    void laSurprimeNoviceEstAppliqueeAvantTroisAns() {
        when(grilles.applicableAu(any())).thenReturn(Optional.of(grilleAvecCoeffsNeutres()));

        // permis obtenu il y a 1 an → novice
        TarifAutoRequest demande = new TarifAutoRequest(
                AUJOURD_HUI.getYear() - 1, new BigDecimal("1.00"), 10000,
                UsageAuto.PRIVE, StationnementAuto.GARAGE_FERME,
                new BigDecimal("8000"), null, null);

        TarifAutoResponse reponse = service.calculer(demande);

        assertThat(reponse.prix().get(0).mensuel()).isEqualByComparingTo("32.50"); // 25 × 1.30
    }

    @Test
    void sansNovicePasDeSurprime() {
        when(grilles.applicableAu(any())).thenReturn(Optional.of(grilleAvecCoeffsNeutres()));

        // permis obtenu il y a 5 ans → non novice
        TarifAutoRequest demande = new TarifAutoRequest(
                AUJOURD_HUI.getYear() - 5, new BigDecimal("1.00"), 10000,
                UsageAuto.PRIVE, StationnementAuto.GARAGE_FERME,
                new BigDecimal("8000"), null, null);

        TarifAutoResponse reponse = service.calculer(demande);

        assertThat(reponse.prix().get(0).mensuel()).isEqualByComparingTo("25.00");
    }

    @Test
    void formuleConseilleeTiersPourUnVehiculeAbordable() {
        when(grilles.applicableAu(any())).thenReturn(Optional.of(grilleAvecCoeffsNeutres()));

        TarifAutoResponse reponse = service.calculer(demandeStandard());

        assertThat(reponse.formuleConseillee()).isEqualTo(FormuleAuto.TIERS.name());
    }

    @Test
    void formuleConseilleeTrPourUnVehiculeHautDeGamme() {
        when(grilles.applicableAu(any())).thenReturn(Optional.of(grilleAvecCoeffsNeutres()));

        TarifAutoRequest demande = new TarifAutoRequest(
                2015, new BigDecimal("1.00"), 10000,
                UsageAuto.PRIVE, StationnementAuto.GARAGE_FERME,
                new BigDecimal("30000"), null, null);

        assertThat(service.calculer(demande).formuleConseillee()).isEqualTo(FormuleAuto.TR.name());
    }

    @Test
    void formuleConseilleInterPourGrandKilometrage() {
        when(grilles.applicableAu(any())).thenReturn(Optional.of(grilleAvecCoeffsNeutres()));

        TarifAutoRequest demande = new TarifAutoRequest(
                2015, new BigDecimal("1.00"), 20000,
                UsageAuto.PRIVE, StationnementAuto.GARAGE_FERME,
                new BigDecimal("8000"), null, null);

        assertThat(service.calculer(demande).formuleConseillee()).isEqualTo(FormuleAuto.INTER.name());
    }

    @Test
    void formuleConseilleInterPourUsageProfessionnel() {
        when(grilles.applicableAu(any())).thenReturn(Optional.of(grilleAvecCoeffsNeutres()));

        TarifAutoRequest demande = new TarifAutoRequest(
                2015, new BigDecimal("1.00"), 10000,
                UsageAuto.PROFESSIONNEL, StationnementAuto.GARAGE_FERME,
                new BigDecimal("8000"), null, null);

        assertThat(service.calculer(demande).formuleConseillee()).isEqualTo(FormuleAuto.INTER.name());
    }

    @Test
    void leDetailEstAbsentSansFormuleChoisie() {
        when(grilles.applicableAu(any())).thenReturn(Optional.of(grilleAvecCoeffsNeutres()));

        assertThat(service.calculer(demandeStandard()).detail()).isNull();
    }

    @Test
    void leDetailEstPresentQuandUneFormuleEstChoisie() {
        when(grilles.applicableAu(any())).thenReturn(Optional.of(grilleAvecCoeffsNeutres()));

        TarifAutoRequest demande = new TarifAutoRequest(
                2015, new BigDecimal("1.00"), 10000,
                UsageAuto.PRIVE, StationnementAuto.GARAGE_FERME,
                new BigDecimal("8000"), FormuleAuto.TIERS, null);

        TarifAutoResponse reponse = service.calculer(demande);

        assertThat(reponse.detail()).isNotNull();
        assertThat(reponse.detail().formule()).isEqualTo("TIERS");
        assertThat(reponse.detail().prixFormule()).isEqualByComparingTo("25.00");
        assertThat(reponse.detail().totalMensuel()).isEqualByComparingTo("25.00");
        assertThat(reponse.detail().totalAnnuel()).isEqualByComparingTo("300.00");
    }

    @Test
    void laDateDeValiditeEstAujourdhuiPlusTrenteJours() {
        when(grilles.applicableAu(any())).thenReturn(Optional.of(grilleAvecCoeffsNeutres()));

        assertThat(service.calculer(demandeStandard()).dateValidite())
                .isEqualTo(AUJOURD_HUI.plusDays(30));
    }
}
