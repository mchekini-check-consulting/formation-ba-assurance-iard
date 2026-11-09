package net.checkconsulting.moteurtarifaire.api.service.critere;

import static net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees.GRILLE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees;
import net.checkconsulting.moteurtarifaire.api.dto.ProfilRisque;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire.Multiplicateurs;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire.Zone;
import net.checkconsulting.moteurtarifaire.api.enums.Dependances;
import net.checkconsulting.moteurtarifaire.api.enums.Etage;
import net.checkconsulting.moteurtarifaire.api.enums.Residence;
import net.checkconsulting.moteurtarifaire.api.enums.StatutOccupant;
import net.checkconsulting.moteurtarifaire.api.enums.TypeLogement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class CritereTarifaireTest {

    private static final Multiplicateurs MULTIPLICATEURS = GRILLE.multiplicateurs();

    @ParameterizedTest
    @EnumSource(value = StatutOccupant.class, names = {"LOCATAIRE", "PROPRIETAIRE_OCCUPANT"})
    void statut(StatutOccupant statut) {
        ProfilRisque profil = JeuxDeDonnees.profilCa1().toBuilder().statut(statut).build();

        assertThat(new CritereStatut().multiplicateur(profil, MULTIPLICATEURS))
                .isEqualByComparingTo(MULTIPLICATEURS.statut().get(statut));
    }

    @ParameterizedTest
    @EnumSource(TypeLogement.class)
    void typeDeLogement(TypeLogement type) {
        ProfilRisque profil = JeuxDeDonnees.profilCa1().toBuilder().typeLogement(type).build();

        assertThat(new CritereTypeLogement().multiplicateur(profil, MULTIPLICATEURS))
                .isEqualByComparingTo(MULTIPLICATEURS.typeLogement().get(type));
    }

    @ParameterizedTest
    @EnumSource(Residence.class)
    void residence(Residence residence) {
        ProfilRisque profil = JeuxDeDonnees.profilCa1().toBuilder().residence(residence).build();

        assertThat(new CritereResidence().multiplicateur(profil, MULTIPLICATEURS))
                .isEqualByComparingTo(MULTIPLICATEURS.residence().get(residence));
    }

    @ParameterizedTest
    @MethodSource("nombresDeSinistres")
    void sinistres(int nombre) {
        ProfilRisque profil = JeuxDeDonnees.profilCa1().toBuilder().sinistres3ans(nombre).build();

        assertThat(new CritereSinistres().multiplicateur(profil, MULTIPLICATEURS))
                .isEqualByComparingTo(MULTIPLICATEURS.sinistres().get(nombre));
    }

    @ParameterizedTest
    @EnumSource(Etage.class)
    void etageDUnAppartement(Etage etage) {
        ProfilRisque profil = JeuxDeDonnees.profilCa1().toBuilder().typeLogement(TypeLogement.APPARTEMENT).etage(etage).build();

        assertThat(new CritereEtage().multiplicateur(profil, MULTIPLICATEURS))
                .isEqualByComparingTo(MULTIPLICATEURS.etage().get(etage));
    }

    @Test
    void lEtageNeCompteQuePourUnAppartement() {
        ProfilRisque maison = JeuxDeDonnees.profilCa1().toBuilder().typeLogement(TypeLogement.MAISON).etage(null).build();

        assertThat(new CritereEtage().multiplicateur(maison, MULTIPLICATEURS)).isEqualByComparingTo(CritereTarifaire.NEUTRE);
    }

    @ParameterizedTest
    @EnumSource(Dependances.class)
    void dependancesDUneMaison(Dependances dependances) {
        ProfilRisque profil = JeuxDeDonnees.profilCa1().toBuilder()
                .typeLogement(TypeLogement.MAISON)
                .dependances(dependances)
                .build();

        assertThat(new CritereDependances().multiplicateur(profil, MULTIPLICATEURS))
                .isEqualByComparingTo(MULTIPLICATEURS.dependances().get(dependances));
    }

    @Test
    void lesDependancesNeComptentQuePourUneMaison() {
        ProfilRisque appartement = JeuxDeDonnees.profilCa1().toBuilder()
                .typeLogement(TypeLogement.APPARTEMENT)
                .dependances(null)
                .build();

        assertThat(new CritereDependances().multiplicateur(appartement, MULTIPLICATEURS))
                .isEqualByComparingTo(CritereTarifaire.NEUTRE);
    }

    @ParameterizedTest
    @MethodSource("departementsMajores")
    void chaqueDepartementMajoreRecoitLeCoefficientDeSaZone(String departement, BigDecimal coefficient) {
        ProfilRisque profil = JeuxDeDonnees.profilCa1().toBuilder().codePostal(departement + "000").build();

        assertThat(new CritereZone().multiplicateur(profil, MULTIPLICATEURS)).isEqualByComparingTo(coefficient);
    }

    @Test
    void unCodePostalHorsZoneNEstPasMajore() {
        assertThat(new CritereZone().multiplicateur(JeuxDeDonnees.profilNeutre(), MULTIPLICATEURS))
                .isEqualByComparingTo(CritereTarifaire.NEUTRE);
    }

    @Test
    void laZoneSeLitAuDebutDuCodePostalPasAilleurs() {
        List<String> departements = MULTIPLICATEURS.zones().getFirst().departements();
        String codeContenantUnDepartementEnFin = "010" + departements.getFirst();
        ProfilRisque profil = JeuxDeDonnees.profilCa1().toBuilder().codePostal(codeContenantUnDepartementEnFin).build();

        assertThat(new CritereZone().multiplicateur(profil, MULTIPLICATEURS)).isEqualByComparingTo(CritereTarifaire.NEUTRE);
    }

    static Stream<Integer> nombresDeSinistres() {
        return MULTIPLICATEURS.sinistres().keySet().stream();
    }

    static Stream<Arguments> departementsMajores() {
        return MULTIPLICATEURS.zones().stream().flatMap(CritereTarifaireTest::departementsDeLaZone);
    }

    private static Stream<Arguments> departementsDeLaZone(Zone zone) {
        return zone.departements().stream()
                .map(departement -> Arguments.of(departement, zone.coefficient()));
    }
}
