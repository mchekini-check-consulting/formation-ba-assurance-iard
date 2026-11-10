package net.checkconsulting.assuranceiard.api.service;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Lance un traitement après la réponse HTTP (US-07, RG1 : même délai de réponse que le compte existe ou non).
 * La partie base de données s'exécute dans sa propre transaction, validée avant la suite (envoi d'e-mail).
 * Un échec est journalisé sans son message, qui pourrait contenir une adresse ou un lien.
 */
@Slf4j
@Component
public class TraitementEnArrierePlan {

    private final Executor executeur;
    private final TransactionOperations transaction;

    public TraitementEnArrierePlan(
            @Qualifier("applicationTaskExecutor") Executor executeur, TransactionOperations transaction) {
        this.executeur = executeur;
        this.transaction = transaction;
    }

    /** Exécute {@code enBase} dans une transaction puis, une fois validée, {@code ensuite} avec son résultat. */
    public <T> void lancer(String nom, Supplier<T> enBase, Consumer<T> ensuite) {
        executeur.execute(() -> executer(nom, enBase, ensuite));
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private <T> void executer(String nom, Supplier<T> enBase, Consumer<T> ensuite) {
        try {
            T resultat = transaction.execute(etat -> enBase.get());
            ensuite.accept(resultat);
        } catch (RuntimeException exception) {
            log.error("Traitement en arrière-plan « {} » non abouti ({})", nom, exception.getClass().getSimpleName());
        }
    }
}
