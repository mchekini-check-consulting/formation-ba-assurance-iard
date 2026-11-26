package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * US-23, AC3 : ce qui change (ancienne et nouvelle cotisation, date d'effet), le projet d'avenant et les documents
 * à relire avant d'accepter.
 *
 * @param frequencePaiement MENSUEL ou ANNUEL
 * @param premiereEcheanceConcernee RG3 : première échéance à la nouvelle cotisation (mensuelle : la première après
 *     la date d'effet ; annuelle : la prochaine échéance annuelle) ; vide si le contrat n'a plus d'échéance
 * @param reductionCouverture RG4 : vrai si une garantie est retirée
 * @param conseils RG4 : conseil mis à jour, affiché avant l'acceptation quand la couverture diminue
 * @param documents documents du contrat à relire (fiche conseil, IPID, conditions générales)
 * @param ipidFormule auto : formule dont l'IPID se télécharge par /documents-contrat/ipid/{formule} ; vide sinon
 */
public record ProjetAvenantResponse(
        AvenantResponse avenant,
        BigDecimal ancienneCotisationAnnuelle,
        BigDecimal nouvelleCotisationAnnuelle,
        String frequencePaiement,
        LocalDate premiereEcheanceConcernee,
        boolean reductionCouverture,
        List<String> conseils,
        List<FicheContratResponse.Document> documents,
        String ipidFormule) {

    public ProjetAvenantResponse {
        conseils = List.copyOf(conseils);
        documents = List.copyOf(documents);
    }
}
