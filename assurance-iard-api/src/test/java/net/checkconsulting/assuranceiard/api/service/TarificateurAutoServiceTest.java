package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.client.MoteurTarifaireClient;
import net.checkconsulting.assuranceiard.api.dto.TarifAutoRequest;
import net.checkconsulting.assuranceiard.api.dto.TarifAutoResponse;
import net.checkconsulting.assuranceiard.api.exception.VehiculeIntrouvableException;
import net.checkconsulting.assuranceiard.api.model.RefPlaqueTest;
import net.checkconsulting.assuranceiard.api.model.RefVehicule;
import net.checkconsulting.assuranceiard.api.repository.RefPlaqueTestRepository;
import net.checkconsulting.assuranceiard.api.repository.RefVehiculeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TarificateurAutoServiceTest {

    @Mock
    private MoteurTarifaireClient client;

    @Mock
    private RefVehiculeRepository vehicules;

    @Mock
    private RefPlaqueTestRepository plaques;

    @InjectMocks
    private TarificateurAutoService service;

    private RefVehicule vehiculePeugeot() {
        RefVehicule v = new RefVehicule();
        v.setId(1L);
        v.setMarque("Peugeot");
        v.setModele("308");
        v.setEnergie("ESSENCE");
        v.setValeurNeuve(new BigDecimal("18500.00"));
        return v;
    }

    private TarifAutoResponse reponseMoteur() {
        return new TarifAutoResponse(UUID.randomUUID(), "AUTO-2026.1", true,
                "INTER", null, null, null, null, null, null, null);
    }

    private TarifAutoRequest demandeAvecId(Long refVehiculeId) {
        return new TarifAutoRequest(null, refVehiculeId,
                2015, new BigDecimal("1.00"), 10000, "PRIVE", "GARAGE_FERME", null, null);
    }

    private TarifAutoRequest demandeAvecPlaque(String immatriculation) {
        return new TarifAutoRequest(immatriculation, null,
                2015, new BigDecimal("1.00"), 10000, "PRIVE", "GARAGE_FERME", null, null);
    }

    @Test
    void resolutionParRefVehiculeId() {
        when(vehicules.findById(1L)).thenReturn(Optional.of(vehiculePeugeot()));
        when(client.calculerAuto(any())).thenReturn(reponseMoteur());

        TarifAutoResponse reponse = service.estimer(demandeAvecId(1L));

        assertThat(reponse.tarifable()).isTrue();
        assertThat(reponse.vehiculeMarque()).isEqualTo("Peugeot");
        assertThat(reponse.vehiculeModele()).isEqualTo("308");
        assertThat(reponse.valeurVehicule()).isEqualByComparingTo("18500.00");
    }

    @Test
    void resolutionParImmatriculation() {
        RefPlaqueTest plaque = new RefPlaqueTest();
        plaque.setImmatriculation("AB-123-CD");
        plaque.setRefVehiculeId(1L);
        plaque.setComportement("TROUVE");

        when(plaques.findByImmatriculation("AB-123-CD")).thenReturn(Optional.of(plaque));
        when(vehicules.findById(1L)).thenReturn(Optional.of(vehiculePeugeot()));
        when(client.calculerAuto(any())).thenReturn(reponseMoteur());

        TarifAutoResponse reponse = service.estimer(demandeAvecPlaque("AB-123-CD"));

        assertThat(reponse.vehiculeMarque()).isEqualTo("Peugeot");
    }

    @Test
    void uneImmatriculationIntrouvableLeveUneException() {
        when(plaques.findByImmatriculation("XX-000-XX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.estimer(demandeAvecPlaque("XX-000-XX")))
                .isInstanceOf(VehiculeIntrouvableException.class);
    }

    @Test
    void unComportementNonTrouveLeveUneException() {
        RefPlaqueTest plaque = new RefPlaqueTest();
        plaque.setImmatriculation("XX-000-XX");
        plaque.setComportement("INCONNU");

        when(plaques.findByImmatriculation("XX-000-XX")).thenReturn(Optional.of(plaque));

        assertThatThrownBy(() -> service.estimer(demandeAvecPlaque("XX-000-XX")))
                .isInstanceOf(VehiculeIntrouvableException.class);
    }

    @Test
    void unRefVehiculeIdInconnuLeveUneException() {
        when(vehicules.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.estimer(demandeAvecId(99L)))
                .isInstanceOf(VehiculeIntrouvableException.class);
    }

    @Test
    void sansIdentifiantDeVehiculeUneExceptionEstLevee() {
        TarifAutoRequest demande = new TarifAutoRequest(null, null,
                2015, new BigDecimal("1.00"), 10000, "PRIVE", "GARAGE_FERME", null, null);

        assertThatThrownBy(() -> service.estimer(demande))
                .isInstanceOf(VehiculeIntrouvableException.class);
    }

    @Test
    void laReponseContientLInfoVehicule() {
        when(vehicules.findById(1L)).thenReturn(Optional.of(vehiculePeugeot()));
        when(client.calculerAuto(any())).thenReturn(reponseMoteur());

        TarifAutoResponse reponse = service.estimer(demandeAvecId(1L));

        assertThat(reponse.vehiculeMarque()).isEqualTo("Peugeot");
        assertThat(reponse.vehiculeModele()).isEqualTo("308");
        assertThat(reponse.valeurVehicule()).isEqualByComparingTo("18500.00");
        assertThat(reponse.versionGrille()).isEqualTo("AUTO-2026.1");
    }
}
