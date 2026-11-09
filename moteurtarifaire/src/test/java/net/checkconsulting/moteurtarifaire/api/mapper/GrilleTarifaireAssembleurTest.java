package net.checkconsulting.moteurtarifaire.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleInvalideException;
import net.checkconsulting.moteurtarifaire.api.model.ParametreTarifaire;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire;
import net.checkconsulting.moteurtarifaire.api.enums.Formule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GrilleTarifaireAssembleurTest {

    private final GrilleTarifaireAssembleur assembleur = new GrilleTarifaireAssembleur();

    private GrilleTarifaire assembler(List<ParametreTarifaire> parametres) {
        return assembleur.assembler(JeuxDeDonnees.versionDeReference(), parametres);
    }

    private List<ParametreTarifaire> avecValeur(String categorie, String cle, String valeur) {
        List<ParametreTarifaire> parametres = new ArrayList<>(JeuxDeDonnees.parametresDeReference());
        parametres.stream()
                .filter(parametre -> parametre.getCategorie().equals(categorie) && parametre.getCle().equals(cle))
                .forEach(parametre -> parametre.setValeur(valeur));
        return parametres;
    }

    @Test
    void reprendLaVersionEtSesDates() {
        GrilleTarifaire grille = assembler(JeuxDeDonnees.parametresDeReference());

        assertThat(grille.version()).isEqualTo(JeuxDeDonnees.versionDeReference().getVersion());
        assertThat(grille.dateDebut()).isEqualTo(JeuxDeDonnees.versionDeReference().getDateDebut());
        assertThat(grille.dateFin()).isEqualTo(JeuxDeDonnees.versionDeReference().getDateFin());
    }

    @Test
    void convertitLesValeursDeLaTable() {
        String prix = "12.34";

        GrilleTarifaire grille = assembler(avecValeur("prix-base", Formule.CONFORT.name(), prix));

        assertThat(grille.prixBase().get(Formule.CONFORT)).isEqualByComparingTo(prix);
    }

    @Test
    void regroupeLesDepartementsDeMemeCoefficientEnUneZone() {
        ParametreTarifaire premier = JeuxDeDonnees.parametre("zone", "11", "9");
        ParametreTarifaire second = JeuxDeDonnees.parametre("zone", "22", "9");
        ParametreTarifaire autre = JeuxDeDonnees.parametre("zone", "33", "8");

        List<GrilleTarifaire.Zone> zones = assembler(List.of(premier, second, autre)).multiplicateurs().zones();

        assertThat(zones).hasSize(2);
        assertThat(zones).filteredOn(zone -> zone.coefficient().compareTo(BigDecimal.valueOf(9)) == 0)
                .singleElement()
                .satisfies(zone -> assertThat(zone.departements()).containsExactlyInAnyOrder("11", "22"));
    }

    @ParameterizedTest
    @CsvSource({
        "prix-base, CONFORT, pas-un-nombre",
        "franchise, 150, pas-un-nombre",
        "general, duree-validite-jours, 1.5",
        "arrondi, mode, INCONNU",
        "zone, 75, pas-un-nombre"
    })
    void uneValeurIllisibleEstSignalee(String categorie, String cle, String valeur) {
        List<ParametreTarifaire> parametres = avecValeur(categorie, cle, valeur);

        assertThatThrownBy(() -> assembler(parametres))
                .isInstanceOf(GrilleInvalideException.class)
                .hasMessageContaining(JeuxDeDonnees.versionDeReference().getVersion());
    }

    @Test
    void uneCleInconnueEstSignalee() {
        List<ParametreTarifaire> parametres = new ArrayList<>(JeuxDeDonnees.parametresDeReference());
        parametres.add(JeuxDeDonnees.parametre("prix-base", "FORMULE_INCONNUE", "1"));

        assertThatThrownBy(() -> assembler(parametres)).isInstanceOf(GrilleInvalideException.class);
    }
}
