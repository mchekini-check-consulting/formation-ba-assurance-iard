package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

class TraitementEnArrierePlanTest {

    private final List<Runnable> enAttente = new ArrayList<>();
    private final TraitementEnArrierePlan arrierePlan =
            new TraitementEnArrierePlan(enAttente::add, TransactionOperations.withoutTransaction());

    @Test
    void neFaitRienAvantLExecutionEnArrierePlan() {
        List<String> etapes = new ArrayList<>();

        arrierePlan.lancer("essai", () -> etapes.add("base"), resultat -> etapes.add("ensuite"));

        assertThat(etapes).isEmpty();
        enAttente.forEach(Runnable::run);
        assertThat(etapes).containsExactly("base", "ensuite");
    }

    @Test
    void passeLeResultatDeLaPartieEnBaseALaSuite() {
        List<String> recus = new ArrayList<>();

        arrierePlan.lancer("essai", () -> "lien", recus::add);
        enAttente.forEach(Runnable::run);

        assertThat(recus).containsExactly("lien");
    }

    @Test
    void unEchecEstJournaliseSansRemonter() {
        List<String> recus = new ArrayList<>();
        arrierePlan.<String>lancer("essai", () -> {
            throw new IllegalStateException("marie@exemple.fr");
        }, recus::add);

        assertThatCode(() -> enAttente.forEach(Runnable::run)).doesNotThrowAnyException();
        assertThat(recus).isEmpty();
    }
}
