package net.checkconsulting.assuranceiard.api.service.document;

import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * US-21, AC4 : à la signature d'un contrat habitation (parcours du sprint 1), crée la version 1 de ses conditions
 * particulières avec le service de documents de T-03. La création a lieu après l'enregistrement de la signature, dans
 * sa propre transaction : si elle échoue, la signature reste valable. Si seul le PDF n'a pas pu être produit, la ligne
 * « non stockée » est reprise par la tâche de reprise de T-03.
 */
@Slf4j
@Component
public class ConditionsParticulieresALaSignature {

    static final String MOTIF = MotifRegeneration.SIGNATURE.name();
    private static final String CONDITIONS_PARTICULIERES = TypeDocument.CONDITIONS_PARTICULIERES.name();

    private final DocumentRepository documents;
    private final DocumentsGeneresService service;
    private final TransactionTemplate nouvelleTransaction;

    public ConditionsParticulieresALaSignature(DocumentRepository documents, DocumentsGeneresService service,
            PlatformTransactionManager transactions) {
        this.documents = documents;
        this.service = service;
        this.nouvelleTransaction = new TransactionTemplate(transactions);
        this.nouvelleTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Programme la création pour après l'enregistrement de la signature (tout de suite hors transaction). */
    @LogMetier
    public void creerApresSignature(long contratId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    creer(contratId);
                }
            });
        } else {
            creer(contratId);
        }
    }

    /** Crée la V1 si le contrat n'a pas encore de conditions particulières ; n'échoue jamais. */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    void creer(long contratId) {
        try {
            nouvelleTransaction.executeWithoutResult(etat -> {
                if (documents.enVigueur(contratId, CONDITIONS_PARTICULIERES, "").isEmpty()) {
                    service.generer(DemandeDocument.duContrat(TypeDocument.CONDITIONS_PARTICULIERES, contratId,
                            MOTIF));
                }
            });
        } catch (RuntimeException erreur) {
            log.warn("Conditions particulières non créées à la signature : contrat={} ({})", contratId,
                    erreur.getClass().getSimpleName());
        }
    }
}
