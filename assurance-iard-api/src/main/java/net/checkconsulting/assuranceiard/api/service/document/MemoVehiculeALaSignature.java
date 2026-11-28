package net.checkconsulting.assuranceiard.api.service.document;

import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * US-33 (§4) : à la signature d'un contrat auto, crée la version 1 du mémo véhicule assuré dans sa propre
 * transaction. Si elle échoue, la signature reste valable.
 */
@Slf4j
@Component
public class MemoVehiculeALaSignature {

    static final String MOTIF = MotifRegeneration.SIGNATURE.name();
    private static final String MEMO_VEHICULE = TypeDocument.MEMO_VEHICULE.name();

    private final DocumentRepository documents;
    private final DocumentsGeneresService service;
    private final TransactionTemplate nouvelleTransaction;

    public MemoVehiculeALaSignature(DocumentRepository documents, DocumentsGeneresService service,
            PlatformTransactionManager transactions) {
        this.documents = documents;
        this.service = service;
        this.nouvelleTransaction = new TransactionTemplate(transactions);
        this.nouvelleTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Programme la création pour après l'enregistrement de la signature (tout de suite hors transaction). */
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

    /** Crée la V1 si le contrat n'a pas encore de mémo véhicule ; n'échoue jamais. */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    void creer(long contratId) {
        try {
            nouvelleTransaction.executeWithoutResult(etat -> {
                if (documents.enVigueur(contratId, MEMO_VEHICULE, "").isEmpty()) {
                    service.generer(DemandeDocument.duContrat(TypeDocument.MEMO_VEHICULE, contratId, MOTIF));
                }
            });
        } catch (RuntimeException erreur) {
            log.warn("Mémo véhicule non créé à la signature : contrat={} ({})", contratId,
                    erreur.getClass().getSimpleName());
        }
    }
}
