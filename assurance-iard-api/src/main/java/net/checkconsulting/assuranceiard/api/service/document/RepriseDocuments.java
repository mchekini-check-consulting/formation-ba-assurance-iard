package net.checkconsulting.assuranceiard.api.service.document;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.config.DocumentsProperties;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * T-03 : toutes les 5 minutes environ,
 * <ol>
 *   <li>crée les conditions générales provisoires de chaque produit si elles n'existent pas encore ;</li>
 *   <li>stocke les documents pas encore stockés (échec de stockage, conditions particulières du sprint 1 créées
 *       par V27, données de recette), à leur version actuelle : jamais de nouvelle version.</li>
 * </ol>
 * Un document par transaction, verrouillé « SKIP LOCKED » : deux instances du backend ne traitent jamais le même.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepriseDocuments {

    static final String MOTIF_CG = "Conditions générales provisoires (T-03)";

    private final DocumentRepository documents;
    private final DocumentsGeneresService service;
    private final CatalogueModeles catalogue;
    private final DocumentsProperties reglages;
    private final TransactionOperations transaction;

    @Scheduled(fixedDelayString = "${assurance.documents.reprise.intervalle:PT5M}",
            initialDelayString = "${assurance.documents.reprise.delai-initial:PT1M}")
    public void planifiee() {
        if (reglages.reprise().active()) {
            reprendre();
        }
    }

    /** Un passage de la reprise ; renvoie le nombre de documents stockés. */
    public int reprendre() {
        ModeleConditionsGenerales.FORMULES_PAR_PRODUIT.keySet().stream().sorted()
                .forEach(this::creerConditionsGenerales);
        ModeleIPIDauto.FORMULES_AUTO.stream().sorted().forEach(this::creerIPIDauto);
        List<Long> aStocker = documents.aStocker(catalogue.typesGeres(), reglages.reprise().lot());
        int stockes = 0;
        for (Long id : aStocker) {
            Boolean stocke = transaction.execute(etat -> documents.aStockerVerrouille(id)
                    .map(service::stocker)
                    .orElse(false));
            if (Boolean.TRUE.equals(stocke)) {
                stockes++;
            }
        }
        if (!aStocker.isEmpty()) {
            log.info("Reprise des documents : {} stockés sur {} en attente", stockes, aStocker.size());
        }
        return stockes;
    }

    private void creerConditionsGenerales(String produit) {
        String type = TypeDocument.CONDITIONS_GENERALES.name();
        if (documents.enVigueur(0, type, produit).isPresent()) {
            return;
        }
        try {
            transaction.executeWithoutResult(etat -> service.generer(
                    DemandeDocument.commun(TypeDocument.CONDITIONS_GENERALES, produit, MOTIF_CG)));
        } catch (DataIntegrityViolationException dejaCreees) {
            log.info("Conditions générales {} déjà créées par une autre instance", produit);
        }
    }

    private void creerIPIDauto(String formule) {
        String type = TypeDocument.IPID.name();
        if (documents.enVigueur(0, type, formule).isPresent()) {
            return;
        }
        try {
            transaction.executeWithoutResult(etat -> service.generer(
                    DemandeDocument.commun(TypeDocument.IPID, formule, "IPID auto provisoire (US-29)")));
        } catch (DataIntegrityViolationException dejaCreees) {
            log.info("IPID auto {} déjà créé par une autre instance", formule);
        }
    }
}
