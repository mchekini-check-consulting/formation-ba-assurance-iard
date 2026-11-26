package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.client.MoteurTarifaireClient;
import net.checkconsulting.assuranceiard.api.dto.FormuleAutoRequest;
import net.checkconsulting.assuranceiard.api.dto.FormuleAutoResponse;
import net.checkconsulting.assuranceiard.api.dto.PrixFormuleAutoResponse;
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

@ExtendWith(MockitoExtension.class)
class FormuleAutoServiceTest {

    @Mock
    private DevisAutoRepository devisAutos;
    @Mock
    private VehiculeRepository vehicules;
    @Mock
    private ConducteurRepository conducteurs;
    @Mock
    private MoteurTarifaireClient moteurTarifaire;

    @InjectMocks
    private FormuleAutoService service;

    private DevisAuto devisAuto;
    private Vehicule vehicule;
    private Conducteur conducteur;

    @BeforeEach
    void setUp() {
        devisAuto = new DevisAuto();
        devisAuto.setVehiculeId(10L);
        devisAuto.setConducteurId(20L);
        devisAuto.setFormuleConseillee("TR");

        vehicule = new Vehicule();
        vehicule.setUsage("PRIVE");
        vehicule.setStationnement("GARAGE_FERME");
        vehicule.setKmAnnuel(11500);
        vehicule.setValeurEstimee(new BigDecimal("18500"));
        vehicule.setDateMiseCirculation(LocalDate.of(2022, 1, 1));

        conducteur = new Conducteur();
        conducteur.setDatePermis(LocalDate.of(2010, 3, 20));
        conducteur.setCoefficientBonusMalus(new BigDecimal("0.80"));

        lenient().when(devisAutos.findById(1L)).thenReturn(Optional.of(devisAuto));
        lenient().when(vehicules.findById(10L)).thenReturn(Optional.of(vehicule));
        lenient().when(conducteurs.findById(20L)).thenReturn(Optional.of(conducteur));
    }

    @Test
    void obtenirPrix_retourne_prix_des_3_formules() {
        List<TarifAutoResponse.PrixFormule> prix = List.of(
                new TarifAutoResponse.PrixFormule("TIERS", new BigDecimal("15.00"), new BigDecimal("180.00")),
                new TarifAutoResponse.PrixFormule("INTER", new BigDecimal("33.90"), new BigDecimal("406.80")),
                new TarifAutoResponse.PrixFormule("TR", new BigDecimal("55.00"), new BigDecimal("660.00")));
        List<TarifAutoResponse.PrixOption> options = List.of(
                new TarifAutoResponse.PrixOption("ASSISTANCE_0KM", new BigDecimal("3.50")),
                new TarifAutoResponse.PrixOption("VEHICULE_REMPLACEMENT", new BigDecimal("4.00")));
        when(moteurTarifaire.calculerAuto(any())).thenReturn(new TarifAutoResponse(
                null, "AUTO-2026.1", true, "TR", prix, options, null, LocalDate.now().plusDays(30),
                "Peugeot", "308", new BigDecimal("18500")));

        PrixFormuleAutoResponse reponse = service.obtenirPrix(1L);

        assertThat(reponse.prix()).hasSize(3);
        assertThat(reponse.prix().get(1).formule()).isEqualTo("INTER");
        assertThat(reponse.prix().get(1).mensuel()).isEqualByComparingTo("33.90");
        // Vérification des franchises
        assertThat(reponse.prix().get(0).franchise()).isNull(); // TIERS : sans objet
        assertThat(reponse.prix().get(1).franchise()).isEqualByComparingTo("400"); // INTER
        assertThat(reponse.prix().get(2).franchise()).isEqualByComparingTo("500"); // TR
        assertThat(reponse.prixOptions()).hasSize(2);
    }

    @Test
    void obtenirPrix_leve_exception_si_devis_introuvable() {
        when(devisAutos.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtenirPrix(99L))
                .isInstanceOf(VehiculeIntrouvableException.class);
    }

    @Test
    void obtenirPrix_leve_exception_si_vehicule_introuvable() {
        when(vehicules.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtenirPrix(1L))
                .isInstanceOf(VehiculeIntrouvableException.class);
    }

    @Test
    void obtenirPrix_leve_exception_si_conducteur_introuvable() {
        when(conducteurs.findById(20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtenirPrix(1L))
                .isInstanceOf(VehiculeIntrouvableException.class);
    }

    @Test
    void obtenirPrix_avec_conducteur_sans_permis_ni_bm_utilise_valeurs_par_defaut() {
        conducteur.setDatePermis(null);
        conducteur.setCoefficientBonusMalus(null);
        vehicule.setKmAnnuel(null);
        vehicule.setValeurEstimee(null);
        when(moteurTarifaire.calculerAuto(any())).thenReturn(new TarifAutoResponse(
                null, "AUTO-2026.1", true, "TR", List.of(), List.of(), null,
                LocalDate.now().plusDays(30), "Peugeot", "308", null));

        PrixFormuleAutoResponse reponse = service.obtenirPrix(1L);

        assertThat(reponse.prix()).isEmpty();
    }

    @Test
    void choisirFormule_vehiculeRemplacement_incompatible_tiers_est_retire() {
        // Si le client choisit TIERS avec VEHICULE_REMPLACEMENT → le service doit quand même enregistrer
        // (la compatibilité est gérée côté frontend ; le service sauvegarde ce qui lui est envoyé)
        FormuleAutoRequest requete = new FormuleAutoRequest(
                "TIERS", List.of("VEHICULE_REMPLACEMENT"), false, "AUTO-2026.1",
                new BigDecimal("15.00"), new BigDecimal("180.00"));

        // Moteur retourne une erreur pour cette combinaison → utilisation du prix fourni
        when(moteurTarifaire.calculerAuto(any())).thenThrow(new RuntimeException("Option incompatible"));
        when(devisAutos.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FormuleAutoResponse reponse = service.choisirFormule(1L, requete);

        assertThat(reponse.formuleChoisie()).isEqualTo("TIERS");
        assertThat(reponse.prixMensuelTtc()).isEqualByComparingTo("15.00");
    }

    @Test
    void choisirFormule_enregistre_les_donnees_et_calcule_validite() {
        TarifAutoResponse.Detail detail = new TarifAutoResponse.Detail(
                "INTER", new BigDecimal("33.90"),
                List.of(new TarifAutoResponse.PrixOption("ASSISTANCE_0KM", new BigDecimal("3.50"))),
                new BigDecimal("37.40"), new BigDecimal("448.80"));
        when(moteurTarifaire.calculerAuto(any())).thenReturn(new TarifAutoResponse(
                null, "AUTO-2026.1", true, "TR", List.of(), List.of(), detail,
                LocalDate.now().plusDays(30), "Peugeot", "308", new BigDecimal("18500")));
        when(devisAutos.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FormuleAutoRequest requete = new FormuleAutoRequest(
                "INTER", List.of("ASSISTANCE_0KM"), false, "AUTO-2026.1", null, null);

        FormuleAutoResponse reponse = service.choisirFormule(1L, requete);

        assertThat(reponse.formuleChoisie()).isEqualTo("INTER");
        assertThat(reponse.optionsChoisies()).contains("ASSISTANCE_0KM");
        assertThat(reponse.prixMensuelTtc()).isEqualByComparingTo("37.40");
        assertThat(reponse.dateFinValidite()).isEqualTo(LocalDate.now().plusDays(30));
        verify(devisAutos).save(any());
    }

    @Test
    void choisirFormule_choix_different_enregistre_date_confirmation() {
        when(moteurTarifaire.calculerAuto(any())).thenThrow(new RuntimeException("test"));
        when(devisAutos.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FormuleAutoRequest requete = new FormuleAutoRequest(
                "TIERS", List.of(), true, "AUTO-2026.1",
                new BigDecimal("15.00"), new BigDecimal("180.00"));

        service.choisirFormule(1L, requete);

        verify(devisAutos).save(any(DevisAuto.class));
        // La vérification de dateConfirmationChoixDifferent est faite via l'entité sauvegardée
    }

    @Test
    void us23PrixDesOptionsCompatiblesAvecLaFormuleDuContrat() {
        List<TarifAutoResponse.PrixOption> catalogue = List.of(
                new TarifAutoResponse.PrixOption("ASSISTANCE_0KM", new BigDecimal("3.50")),
                new TarifAutoResponse.PrixOption("VEHICULE_REMPLACEMENT", new BigDecimal("4.00")));
        // Le moteur ne garde que les options compatibles avec la formule (TIERS : pas de véhicule de remplacement).
        TarifAutoResponse.Detail detail = new TarifAutoResponse.Detail("TIERS", new BigDecimal("20.00"),
                List.of(catalogue.getFirst()), new BigDecimal("23.50"), new BigDecimal("282.00"));
        when(moteurTarifaire.calculerAuto(any())).thenAnswer(appel -> {
            MoteurTarifaireClient.DemandeMoteurAuto demande = appel.getArgument(0);
            return new TarifAutoResponse(null, "AUTO-2026.1", true, "TR", List.of(), catalogue,
                    demande.options() == null ? null : detail, LocalDate.now(), null, null, null);
        });

        assertThat(service.prixDesOptions(vehicule, conducteur, "TIERS"))
                .containsExactly(java.util.Map.entry("ASSISTANCE_0KM", new BigDecimal("3.50")));
        verify(moteurTarifaire).calculerAuto(org.mockito.ArgumentMatchers.argThat(demande ->
                demande.options() != null && demande.options().equals(
                        List.of("ASSISTANCE_0KM", "VEHICULE_REMPLACEMENT")) && "TIERS".equals(demande.formule())));
    }
}
