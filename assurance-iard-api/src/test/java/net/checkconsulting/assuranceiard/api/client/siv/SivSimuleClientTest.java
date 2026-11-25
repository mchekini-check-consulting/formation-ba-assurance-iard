package net.checkconsulting.assuranceiard.api.client.siv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.RefPlaqueTest;
import net.checkconsulting.assuranceiard.api.model.RefVehicule;
import net.checkconsulting.assuranceiard.api.repository.RefPlaqueTestRepository;
import net.checkconsulting.assuranceiard.api.repository.RefVehiculeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SivSimuleClientTest {

    @Mock
    private RefPlaqueTestRepository plaques;

    @Mock
    private RefVehiculeRepository vehicules;

    private SivSimuleClient client;

    @BeforeEach
    void setUp() {
        client = new SivSimuleClient(plaques, vehicules);
    }

    @Test
    void unePlaqueInconnueRenvoieInconnu() {
        when(plaques.findByImmatriculation("XX-999-XX")).thenReturn(Optional.empty());

        SivReponse reponse = client.consulterPlaque("XX-999-XX");

        assertThat(reponse.statut()).isEqualTo(SivReponse.INCONNU);
        assertThat(reponse.vehicule()).isNull();
    }

    @Test
    void unePlaqueErreurRenvoieErreur() {
        when(plaques.findByImmatriculation("EE-500-EE")).thenReturn(Optional.of(entree("EE-500-EE", SivReponse.ERREUR, null)));

        SivReponse reponse = client.consulterPlaque("EE-500-EE");

        assertThat(reponse.statut()).isEqualTo(SivReponse.ERREUR);
        assertThat(reponse.vehicule()).isNull();
    }

    @Test
    void unePlaqueDelaiRenvoieDelai() {
        when(plaques.findByImmatriculation("DD-408-DD")).thenReturn(Optional.of(entree("DD-408-DD", SivReponse.DELAI, null)));

        SivReponse reponse = client.consulterPlaque("DD-408-DD");

        assertThat(reponse.statut()).isEqualTo(SivReponse.DELAI);
        assertThat(reponse.vehicule()).isNull();
    }

    @Test
    void unePlaqueConnueRenvoieLesInfosVehicule() {
        RefVehicule vehicule = vehicule(42L, "Peugeot", "308", "ESSENCE", "1.2 PureTech", 2021, new BigDecimal("18500"));
        when(plaques.findByImmatriculation("AB-123-CD")).thenReturn(Optional.of(entree("AB-123-CD", SivReponse.TROUVE, 42L)));
        when(vehicules.findById(42L)).thenReturn(Optional.of(vehicule));

        SivReponse reponse = client.consulterPlaque("AB-123-CD");

        assertThat(reponse.statut()).isEqualTo(SivReponse.TROUVE);
        assertThat(reponse.vehicule()).isNotNull();
        assertThat(reponse.vehicule().marque()).isEqualTo("Peugeot");
        assertThat(reponse.vehicule().modele()).isEqualTo("308");
        assertThat(reponse.vehicule().refVehiculeId()).isEqualTo(42L);
    }

    @Test
    void unePlaqueConnueSansVehiculeEnBaseLeveUneException() {
        when(plaques.findByImmatriculation("AB-123-CD")).thenReturn(Optional.of(entree("AB-123-CD", SivReponse.TROUVE, 99L)));
        when(vehicules.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> client.consulterPlaque("AB-123-CD"))
                .isInstanceOf(IllegalStateException.class);
    }

    private RefPlaqueTest entree(String immatriculation, String comportement, Long refVehiculeId) {
        RefPlaqueTest e = new RefPlaqueTest();
        e.setImmatriculation(immatriculation);
        e.setComportement(comportement);
        e.setRefVehiculeId(refVehiculeId);
        return e;
    }

    private RefVehicule vehicule(Long id, String marque, String modele, String energie, String version, int annee, BigDecimal valeur) {
        RefVehicule v = org.mockito.Mockito.mock(RefVehicule.class);
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
