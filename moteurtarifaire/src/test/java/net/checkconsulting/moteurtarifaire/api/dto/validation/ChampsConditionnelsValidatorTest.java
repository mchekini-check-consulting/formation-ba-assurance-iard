package net.checkconsulting.moteurtarifaire.api.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees;
import net.checkconsulting.moteurtarifaire.api.dto.TarifHabitationRequest;
import net.checkconsulting.moteurtarifaire.api.enums.Dependances;
import net.checkconsulting.moteurtarifaire.api.enums.Etage;
import net.checkconsulting.moteurtarifaire.api.enums.TypeLogement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ChampsConditionnelsValidatorTest {

    private static ValidatorFactory usine;
    private static Validator validateur;

    @BeforeAll
    static void creerLeValidateur() {
        usine = Validation.buildDefaultValidatorFactory();
        validateur = usine.getValidator();
    }

    @AfterAll
    static void fermerLeValidateur() {
        usine.close();
    }

    private static TarifHabitationRequest appartement(Etage etage) {
        return JeuxDeDonnees.demandeCa1().toBuilder()
                .typeLogement(TypeLogement.APPARTEMENT)
                .etage(etage)
                .dependances(null)
                .build();
    }

    private static TarifHabitationRequest maison(Dependances dependances) {
        return JeuxDeDonnees.demandeCa1().toBuilder()
                .typeLogement(TypeLogement.MAISON)
                .etage(null)
                .dependances(dependances)
                .build();
    }

    private static Set<ConstraintViolation<TarifHabitationRequest>> violations(TarifHabitationRequest demande) {
        return validateur.validate(demande);
    }

    private static void verifierUneSeuleErreurSur(TarifHabitationRequest demande, String champ, String motif) {
        assertThat(violations(demande)).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath()).hasToString(champ);
            assertThat(violation.getMessage()).contains(motif);
        });
    }

    @ParameterizedTest
    @EnumSource(Etage.class)
    void unAppartementAvecSonEtageEstAccepte(Etage etage) {
        assertThat(violations(appartement(etage))).isEmpty();
    }

    @Test
    void unAppartementSansEtageEstRefuseSurLeChampEtage() {
        verifierUneSeuleErreurSur(appartement(null), "etage", "appartement");
    }

    @ParameterizedTest
    @EnumSource(Dependances.class)
    void uneMaisonAvecSesDependancesEstAcceptee(Dependances dependances) {
        assertThat(violations(maison(dependances))).isEmpty();
    }

    @Test
    void uneMaisonSansDependancesEstRefuseeSurLeChampDependances() {
        verifierUneSeuleErreurSur(maison(null), "dependances", "maison");
    }

    @Test
    void lEtageDUneMaisonEstIgnore() {
        TarifHabitationRequest maisonAvecEtage = maison(Dependances.AUCUNE).toBuilder().etage(Etage.RDC).build();

        assertThat(violations(maisonAvecEtage)).isEmpty();
    }

    @Test
    void lesDependancesDUnAppartementSontIgnorees() {
        TarifHabitationRequest appartementAvecDependances =
                appartement(Etage.RDC).toBuilder().dependances(Dependances.JARDIN).build();

        assertThat(violations(appartementAvecDependances)).isEmpty();
    }

    @Test
    void sansTypeDeLogementSeulLeChampTypeLogementEstSignale() {
        TarifHabitationRequest sansType = appartement(null).toBuilder().typeLogement(null).build();

        verifierUneSeuleErreurSur(sansType, "typeLogement", "obligatoire");
    }
}
