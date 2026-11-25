package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import net.checkconsulting.assuranceiard.api.dto.MarqueResponse;
import net.checkconsulting.assuranceiard.api.model.RefVehicule;
import net.checkconsulting.assuranceiard.api.repository.RefVehiculeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReferentielVehiculeServiceTest {

    @Mock
    private RefVehiculeRepository vehicules;

    private ReferentielVehiculeService service;

    @BeforeEach
    void setUp() {
        service = new ReferentielVehiculeService(vehicules);
    }

    @Test
    void lesVehiculeSontGroupesParMarque() {
        RefVehicule p308 = vehicule(1L, "Peugeot", "308", "ESSENCE", null, 2021, new BigDecimal("18500"));
        RefVehicule p508 = vehicule(2L, "Peugeot", "508", "DIESEL", null, 2022, new BigDecimal("32000"));
        RefVehicule clio = vehicule(3L, "Renault", "Clio", "ESSENCE", null, 2022, new BigDecimal("14200"));
        when(vehicules.findAllByOrderByMarqueAscModeleAsc()).thenReturn(List.of(p308, p508, clio));

        List<MarqueResponse> resultat = service.parMarque();

        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).marque()).isEqualTo("Peugeot");
        assertThat(resultat.get(0).modeles()).hasSize(2);
        assertThat(resultat.get(1).marque()).isEqualTo("Renault");
        assertThat(resultat.get(1).modeles()).hasSize(1);
    }

    @Test
    void lesModelesDUneMarqueConserventLeurOrdre() {
        RefVehicule v208 = vehicule(1L, "Peugeot", "208", "ESSENCE", null, 2020, new BigDecimal("16000"));
        RefVehicule v308 = vehicule(2L, "Peugeot", "308", "DIESEL", null, 2021, new BigDecimal("22000"));
        when(vehicules.findAllByOrderByMarqueAscModeleAsc()).thenReturn(List.of(v208, v308));

        List<MarqueResponse> resultat = service.parMarque();

        assertThat(resultat.getFirst().modeles()).extracting("modele")
                .containsExactly("208", "308");
    }

    @Test
    void sansVehiculeLeResultatEstVide() {
        when(vehicules.findAllByOrderByMarqueAscModeleAsc()).thenReturn(java.util.Collections.emptyList());

        assertThat(service.parMarque()).isEmpty();
    }

    private RefVehicule vehicule(Long id, String marque, String modele, String energie, String version, int annee, BigDecimal valeur) {
        RefVehicule v = Mockito.mock(RefVehicule.class);
        when(v.getId()).thenReturn(id);
        when(v.getMarque()).thenReturn(marque);
        when(v.getModele()).thenReturn(modele);
        when(v.getEnergie()).thenReturn(energie);
        when(v.getVersion()).thenReturn(version);
        when(v.getAnnee()).thenReturn(annee);
        when(v.getValeurNeuve()).thenReturn(valeur);
        return v;
    }
}
