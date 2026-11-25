package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.client.MoteurTarifaireClient;
import net.checkconsulting.assuranceiard.api.dto.BesoinsAutoReponse;
import net.checkconsulting.assuranceiard.api.dto.BesoinsAutoRequest;
import net.checkconsulting.assuranceiard.api.dto.TarifAutoResponse;
import net.checkconsulting.assuranceiard.api.exception.VehiculeIntrouvableException;
import net.checkconsulting.assuranceiard.api.model.Conducteur;
import net.checkconsulting.assuranceiard.api.model.DevisAuto;
import net.checkconsulting.assuranceiard.api.model.Vehicule;
import net.checkconsulting.assuranceiard.api.repository.ConducteurRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisAutoRepository;
import net.checkconsulting.assuranceiard.api.repository.VehiculeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BesoinsAutoServiceTest {

    @Mock
    private DevisAutoRepository devisAutos;
    @Mock
    private VehiculeRepository vehicules;
    @Mock
    private ConducteurRepository conducteurs;
    @Mock
    private MoteurTarifaireClient moteurTarifaire;

    @InjectMocks
    private BesoinsAutoService service;

    private DevisAuto devisAuto;
    private Vehicule vehicule;
    private Conducteur conducteur;

    @BeforeEach
    void setUp() {
        devisAuto = new DevisAuto();
        devisAuto.setId(1L);
        devisAuto.setVehiculeId(10L);
        devisAuto.setConducteurId(20L);

        // Vehicule neutre : 7 ans, 10 000 EUR, COMPTANT — ne declenche pas TOUS_RISQUES ni TIERS par defaut
        vehicule = new Vehicule();
        vehicule.setId(10L);
        vehicule.setKmAnnuel(11500);
        vehicule.setUsage("PRIVE");
        vehicule.setStationnement("GARAGE_FERME");
        vehicule.setValeurEstimee(new BigDecimal("10000"));
        vehicule.setDateMiseCirculation(LocalDate.now().minusYears(7));
        vehicule.setModeAchat("COMPTANT");

        conducteur = new Conducteur();
        conducteur.setId(20L);
        conducteur.setDatePermis(LocalDate.of(2010, 3, 20));
        conducteur.setCoefficientBonusMalus(new BigDecimal("0.90"));

        when(devisAutos.findById(1L)).thenReturn(Optional.of(devisAuto));
        when(vehicules.findById(10L)).thenReturn(Optional.of(vehicule));
        when(conducteurs.findById(20L)).thenReturn(Optional.of(conducteur));
        when(devisAutos.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TarifAutoResponse tarifMock = new TarifAutoResponse(
                null, null, true, "TOUS_RISQUES",
                List.of(new TarifAutoResponse.PrixFormule("TOUS_RISQUES", new BigDecimal("45.00"), new BigDecimal("540.00"))),
                null, null, null, "Peugeot", "308", new BigDecimal("18500"));
        when(moteurTarifaire.calculerAuto(any())).thenReturn(tarifMock);
    }

    // ── RG1 : determination de la formule ──────────────────────────────────────

    @Test
    void rg1_tousRisques_vehiculeRecent() {
        vehicule.setDateMiseCirculation(LocalDate.now().minusYears(4));
        BesoinsAutoRequest r = new BesoinsAutoRequest(10000, true, false, false, false, false, "EQUILIBRE");
        assertThat(service.determinerFormule(r, vehicule)).isEqualTo("TOUS_RISQUES");
    }

    @Test
    void rg1_intermediaire_vehicule5Ans() {
        vehicule.setDateMiseCirculation(LocalDate.now().minusYears(5));
        BesoinsAutoRequest r = new BesoinsAutoRequest(10000, true, false, false, false, false, "EQUILIBRE");
        assertThat(service.determinerFormule(r, vehicule)).isEqualTo("INTERMEDIAIRE");
    }

    @Test
    void rg1_tousRisques_valeurElevee() {
        BesoinsAutoRequest r = new BesoinsAutoRequest(15001, false, false, false, false, false, "EQUILIBRE");
        assertThat(service.determinerFormule(r, vehicule)).isEqualTo("TOUS_RISQUES");
    }

    @Test
    void rg1_intermediaire_valeurExactement15000() {
        BesoinsAutoRequest r = new BesoinsAutoRequest(15000, false, false, false, false, false, "EQUILIBRE");
        assertThat(service.determinerFormule(r, vehicule)).isEqualTo("INTERMEDIAIRE");
    }

    @Test
    void rg1_tousRisques_loa() {
        vehicule.setModeAchat("LOA");
        BesoinsAutoRequest r = new BesoinsAutoRequest(1000, false, false, false, false, false, "PRIX");
        assertThat(service.determinerFormule(r, vehicule)).isEqualTo("TOUS_RISQUES");
    }

    @Test
    void rg1_tousRisques_lld() {
        vehicule.setModeAchat("LLD");
        BesoinsAutoRequest r = new BesoinsAutoRequest(10000, false, false, false, false, false, "EQUILIBRE");
        assertThat(service.determinerFormule(r, vehicule)).isEqualTo("TOUS_RISQUES");
    }

    @Test
    void rg1_tousRisques_protectionLarge() {
        BesoinsAutoRequest r = new BesoinsAutoRequest(10000, false, false, false, false, false, "PROTECTION_LARGE");
        assertThat(service.determinerFormule(r, vehicule)).isEqualTo("TOUS_RISQUES");
    }

    @Test
    void rg1_tiers_prixEtVehiculeAncien() {
        vehicule.setDateMiseCirculation(LocalDate.now().minusYears(11));
        BesoinsAutoRequest r = new BesoinsAutoRequest(10000, false, false, false, false, false, "PRIX");
        assertThat(service.determinerFormule(r, vehicule)).isEqualTo("TIERS");
    }

    @Test
    void rg1_intermediaire_prixEtVehicule10Ans() {
        vehicule.setDateMiseCirculation(LocalDate.now().minusYears(10));
        BesoinsAutoRequest r = new BesoinsAutoRequest(10000, false, false, false, false, false, "PRIX");
        assertThat(service.determinerFormule(r, vehicule)).isEqualTo("INTERMEDIAIRE");
    }

    @Test
    void rg1_tiers_prixEtValeurFaible() {
        BesoinsAutoRequest r = new BesoinsAutoRequest(2999, false, false, false, false, false, "PRIX");
        assertThat(service.determinerFormule(r, vehicule)).isEqualTo("TIERS");
    }

    @Test
    void rg1_intermediaire_prixEtValeur3000() {
        BesoinsAutoRequest r = new BesoinsAutoRequest(3000, false, false, false, false, false, "PRIX");
        assertThat(service.determinerFormule(r, vehicule)).isEqualTo("INTERMEDIAIRE");
    }

    @Test
    void rg1_intermediaire_equilibre() {
        BesoinsAutoRequest r = new BesoinsAutoRequest(10000, false, false, false, false, false, "EQUILIBRE");
        assertThat(service.determinerFormule(r, vehicule)).isEqualTo("INTERMEDIAIRE");
    }

    @Test
    void rg1_garantiesSansEffet() {
        BesoinsAutoRequest r = new BesoinsAutoRequest(10000, true, true, true, true, true, "EQUILIBRE");
        assertThat(service.determinerFormule(r, vehicule)).isEqualTo("INTERMEDIAIRE");
    }

    // ── Enregistrement complet ──────────────────────────────────────────────────

    @Test
    void ca1_enregistrementValideRetourneFormuleEtPrix() {
        BesoinsAutoRequest r = new BesoinsAutoRequest(10000, true, false, false, false, false, "PROTECTION_LARGE");
        BesoinsAutoReponse rep = service.enregistrerBesoins(1L, r);

        assertThat(rep.formuleConseillee()).isEqualTo("TOUS_RISQUES");
        assertThat(rep.raisons()).isNotEmpty();
        assertThat(rep.prixMensuel()).isEqualByComparingTo("45.00");
        verify(devisAutos).save(any(DevisAuto.class));
    }

    @Test
    void ca2_devisIntrouvableLeve404() {
        when(devisAutos.findById(99L)).thenReturn(Optional.empty());
        BesoinsAutoRequest r = new BesoinsAutoRequest(10000, true, false, false, false, false, "EQUILIBRE");
        assertThatThrownBy(() -> service.enregistrerBesoins(99L, r))
                .isInstanceOf(VehiculeIntrouvableException.class);
    }

    @Test
    void ca3_tarifIndisponibleNeBloquePas() {
        when(moteurTarifaire.calculerAuto(any())).thenThrow(new RuntimeException("moteur HS"));
        BesoinsAutoRequest r = new BesoinsAutoRequest(10000, true, false, false, false, false, "PROTECTION_LARGE");
        BesoinsAutoReponse rep = service.enregistrerBesoins(1L, r);

        assertThat(rep.formuleConseillee()).isEqualTo("TOUS_RISQUES");
        assertThat(rep.prixMensuel()).isNull();
    }

    // ── Raisons ────────────────────────────────────────────────────────────────

    @Test
    void raisonsContiennentMotifsTousRisques_vehiculeRecent() {
        vehicule.setDateMiseCirculation(LocalDate.now().minusYears(4));
        BesoinsAutoRequest r = new BesoinsAutoRequest(10000, false, false, false, false, false, "EQUILIBRE");
        List<String> raisons = service.genererRaisons("TOUS_RISQUES", r, vehicule);
        assertThat(raisons).anyMatch(s -> s.contains("moins de 5 ans"));
    }

    @Test
    void raisonsContiennentMotifsTousRisques_protectionLarge() {
        BesoinsAutoRequest r = new BesoinsAutoRequest(10000, false, false, false, false, false, "PROTECTION_LARGE");
        List<String> raisons = service.genererRaisons("TOUS_RISQUES", r, vehicule);
        assertThat(raisons).anyMatch(s -> s.contains("protection la plus large"));
    }

    @Test
    void raisonsContiennentMotifsTiers_prix() {
        vehicule.setDateMiseCirculation(LocalDate.now().minusYears(11));
        BesoinsAutoRequest r = new BesoinsAutoRequest(10000, false, false, false, false, false, "PRIX");
        List<String> raisons = service.genererRaisons("TIERS", r, vehicule);
        assertThat(raisons).anyMatch(s -> s.contains("privilégiez le prix"));
        assertThat(raisons).anyMatch(s -> s.contains("plus de 10 ans"));
    }

    @Test
    void raisonsContiennentMotifIntermediaire() {
        BesoinsAutoRequest r = new BesoinsAutoRequest(10000, false, false, false, false, false, "EQUILIBRE");
        List<String> raisons = service.genererRaisons("INTERMEDIAIRE", r, vehicule);
        assertThat(raisons).hasSize(1);
        assertThat(raisons.get(0)).contains("équilibre");
    }
}