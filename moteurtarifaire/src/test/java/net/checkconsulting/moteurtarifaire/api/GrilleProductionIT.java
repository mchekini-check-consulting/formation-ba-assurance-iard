package net.checkconsulting.moteurtarifaire.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import net.checkconsulting.moteurtarifaire.api.dto.PrixFormuleResponse;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire;
import net.checkconsulting.moteurtarifaire.api.service.GrilleTarifaireService;
import net.checkconsulting.moteurtarifaire.api.service.CalculateurPrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GrilleProductionIT {

    @Autowired
    private GrilleTarifaireService grilles;

    @Autowired
    private CalculateurPrix calculateur;

    @Autowired
    private Clock horloge;

    private GrilleTarifaire grille;

    @BeforeEach
    void charger() {
        grille = grilles.applicableAu(LocalDate.now(horloge)).orElseThrow();
    }

    @Test
    void chaqueFormuleSuperieureCouteStrictementPlusCher() {
        List<BigDecimal> prix = calculateur.prixDesFormules(JeuxDeDonnees.profilCa1(), grille).stream()
                .map(PrixFormuleResponse::mensuel)
                .toList();

        assertThat(prix).isSorted().doesNotHaveDuplicates();
    }

    @Test
    void lePrixLePlusBasResteStrictementPositif() {
        BigDecimal baisseMaximale = grille.ajustementsFranchise().values().stream().min(BigDecimal::compareTo).orElseThrow();
        BigDecimal prixMinimal = calculateur.prixDesFormules(JeuxDeDonnees.profilNeutre(), grille).getFirst().mensuel();

        assertThat(prixMinimal.add(baisseMaximale)).isPositive();
    }

    @Test
    void lesSeuilsDeRecommandationSontDansLeBonOrdre() {
        assertThat(grille.recommandation().seuilContenuEssentielle())
                .isLessThan(grille.recommandation().seuilContenuSerenite());
    }

    @Test
    void tousLesDepartementsSontCouvertsUneSeuleFois() {
        List<String> attendus = new ArrayList<>(IntStream.rangeClosed(1, 95).mapToObj("%02d"::formatted).toList());
        attendus.addAll(List.of("971", "972", "973", "974", "976"));

        List<String> couverts = grille.multiplicateurs().zones().stream()
                .flatMap(zone -> zone.departements().stream())
                .toList();

        assertThat(couverts).containsExactlyInAnyOrderElementsOf(attendus);
    }
}
