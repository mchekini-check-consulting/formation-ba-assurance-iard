package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.CriteresEligibiliteResponse;
import net.checkconsulting.assuranceiard.api.dto.DevisAutoCreationRequest;
import net.checkconsulting.assuranceiard.api.dto.DevisAutoCreationResponse;
import net.checkconsulting.assuranceiard.api.exception.ProfilBloqueException;
import net.checkconsulting.assuranceiard.api.model.Conducteur;
import net.checkconsulting.assuranceiard.api.model.DevisAuto;
import net.checkconsulting.assuranceiard.api.model.RefVehicule;
import net.checkconsulting.assuranceiard.api.model.Vehicule;
import net.checkconsulting.assuranceiard.api.repository.ConducteurRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisAutoRepository;
import net.checkconsulting.assuranceiard.api.repository.RefVehiculeRepository;
import net.checkconsulting.assuranceiard.api.repository.VehiculeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DevisAutoCreationServiceTest {

    @Mock
    private EligibiliteAutoService eligibiliteAutoService;
    @Mock
    private RefVehiculeRepository refVehicules;
    @Mock
    private VehiculeRepository vehicules;
    @Mock
    private ConducteurRepository conducteurs;
    @Mock
    private DevisAutoRepository devisAutos;

    @InjectMocks
    private DevisAutoCreationService service;

    private static final CriteresEligibiliteResponse CRITERES =
            new CriteresEligibiliteResponse(new BigDecimal("1.50"), "NON_PAIEMENT");

    private RefVehicule refPeugeot;

    @BeforeEach
    void setUp() {
        when(eligibiliteAutoService.criteres()).thenReturn(CRITERES);

        refPeugeot = new RefVehicule();
        refPeugeot.setMarque("Peugeot");
        refPeugeot.setModele("308");
        refPeugeot.setEnergie("ESSENCE");
        refPeugeot.setAnnee(2021);
        refPeugeot.setValeurNeuve(new BigDecimal("18500.00"));
    }

    /** Stubs les repositories : à appeler uniquement dans les tests qui atteignent la sauvegarde. */
    private void stubRepositoires() {
        when(refVehicules.findById(1L)).thenReturn(Optional.of(refPeugeot));
        when(vehicules.save(any())).thenAnswer(inv -> {
            Vehicule v = inv.getArgument(0);
            v.setId(10L);
            return v;
        });
        when(conducteurs.save(any())).thenAnswer(inv -> {
            Conducteur c = inv.getArgument(0);
            c.setId(20L);
            return c;
        });
        when(devisAutos.save(any())).thenAnswer(inv -> {
            DevisAuto da = inv.getArgument(0);
            da.setId(100L);
            return da;
        });
    }

    private DevisAutoCreationRequest requeteValide() {
        return new DevisAutoCreationRequest(
                1L, null, false, "SOUSCRIPTEUR", "15/06/2023", "COMPTANT",
                "PRIVE", "GARAGE_FERME", 11500, "75000",
                "MOI_MEME", "15/06/1990", "20/03/2010",
                new BigDecimal("0.90"), 5, false,
                0, 0, "AUCUNE", false, false, "NON", null, null);
    }

    @Test
    void ca1CreationValideEnregistreTroisEntites() {
        stubRepositoires();
        DevisAutoCreationResponse rep = service.creer(requeteValide());

        verify(vehicules).save(any(Vehicule.class));
        verify(conducteurs).save(any(Conducteur.class));
        verify(devisAutos).save(any(DevisAuto.class));
        assertThat(rep.devisAutoId()).isEqualTo(100L);
        assertThat(rep.eligibleEnLigne()).isTrue();
        assertThat(rep.statutDevis()).isEqualTo("EN_COURS");
    }

    @Test
    void ca2BonusMalusDepasseSeuil422() {
        DevisAutoCreationRequest demande = new DevisAutoCreationRequest(
                1L, null, false, "SOUSCRIPTEUR", null, null,
                "PRIVE", "GARAGE_FERME", 11500, "75000",
                "MOI_MEME", "15/06/1990", "20/03/2010",
                new BigDecimal("1.80"), 5, false,
                0, 0, "AUCUNE", false, false, "NON", null, null);

        assertThatThrownBy(() -> service.creer(demande))
                .isInstanceOf(ProfilBloqueException.class)
                .hasMessageContaining("bonus-malus");
    }

    @Test
    void ca3ResiliationNonPaiementBloque() {
        DevisAutoCreationRequest demande = new DevisAutoCreationRequest(
                1L, null, false, "SOUSCRIPTEUR", null, null,
                "PRIVE", "GARAGE_FERME", 11500, "75000",
                "MOI_MEME", "15/06/1990", "20/03/2010",
                new BigDecimal("1.00"), 5, false,
                0, 0, "NON_PAIEMENT", false, false, "NON", null, null);

        assertThatThrownBy(() -> service.creer(demande))
                .isInstanceOf(ProfilBloqueException.class)
                .extracting(e -> ((ProfilBloqueException) e).getMotif())
                .isEqualTo("RESILIATION");
    }

    @Test
    void ca4SuspensionPermisBloque() {
        DevisAutoCreationRequest demande = new DevisAutoCreationRequest(
                1L, null, false, "SOUSCRIPTEUR", null, null,
                "PRIVE", "GARAGE_FERME", 11500, "75000",
                "MOI_MEME", "15/06/1990", "20/03/2010",
                new BigDecimal("1.00"), 5, false,
                0, 0, "AUCUNE", true, false, "NON", null, null);

        assertThatThrownBy(() -> service.creer(demande))
                .isInstanceOf(ProfilBloqueException.class)
                .extracting(e -> ((ProfilBloqueException) e).getMotif())
                .isEqualTo("SUSPENSION_PERMIS");
    }

    @Test
    void ca5NoviceAvecSinistreBloque() {
        // Permis < 3 ans + 1 sinistre responsable
        DevisAutoCreationRequest demande = new DevisAutoCreationRequest(
                1L, null, false, "SOUSCRIPTEUR", null, null,
                "PRIVE", "GARAGE_FERME", 11500, "75000",
                "MOI_MEME", "15/06/2004", "15/09/2024",
                new BigDecimal("1.00"), 1, false,
                1, 0, "AUCUNE", false, false, "NON", null, null);

        assertThatThrownBy(() -> service.creer(demande))
                .isInstanceOf(ProfilBloqueException.class)
                .extracting(e -> ((ProfilBloqueException) e).getMotif())
                .isEqualTo("NOVICE_SINISTRE");
    }

    @Test
    void ca6TitulaireAutrePersonneBloque() {
        DevisAutoCreationRequest demande = new DevisAutoCreationRequest(
                1L, null, false, "AUTRE_PERSONNE", null, null,
                "PRIVE", "GARAGE_FERME", 11500, "75000",
                "MOI_MEME", "15/06/1990", "20/03/2010",
                new BigDecimal("1.00"), 5, false,
                0, 0, "AUCUNE", false, false, "NON", null, null);

        assertThatThrownBy(() -> service.creer(demande))
                .isInstanceOf(ProfilBloqueException.class)
                .extracting(e -> ((ProfilBloqueException) e).getMotif())
                .isEqualTo("TITULAIRE_CARTE_GRISE");
    }

    @Test
    void ca7ResiliationSinistraliteNonBloquante() {
        stubRepositoires();
        DevisAutoCreationRequest demande = new DevisAutoCreationRequest(
                1L, null, false, "SOUSCRIPTEUR", null, null,
                "PRIVE", "GARAGE_FERME", 11500, "75000",
                "MOI_MEME", "15/06/1990", "20/03/2010",
                new BigDecimal("1.00"), 5, false,
                0, 0, "SINISTRALITE", false, false, "NON", null, null);

        DevisAutoCreationResponse rep = service.creer(demande);
        assertThat(rep.eligibleEnLigne()).isTrue();
    }
}
