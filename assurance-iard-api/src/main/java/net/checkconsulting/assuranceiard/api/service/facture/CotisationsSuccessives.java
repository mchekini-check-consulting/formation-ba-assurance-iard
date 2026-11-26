package net.checkconsulting.assuranceiard.api.service.facture;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.checkconsulting.assuranceiard.api.model.Avenant;
import net.checkconsulting.assuranceiard.api.service.avenant.CalculAvenant;
import net.checkconsulting.assuranceiard.api.service.facture.EcheancierService.LigneEcheance;

/**
 * US-23, RG3 : montant de chaque échéance quand des avenants acceptés ont changé la cotisation. Une échéance datée
 * de la date d'effet d'un avenant ou après est à la nouvelle cotisation (paiement mensuel : la première mensualité
 * qui suit la date d'effet, sans prorata ; paiement annuel : la prochaine échéance annuelle) ; avant, à l'ancienne.
 * <ul>
 *   <li>Échéances calculées : montant de la cotisation en vigueur à leur date.</li>
 *   <li>Échéances enregistrées (table echeance) : seules celles « À venir » à partir de la date d'effet d'un avenant
 *       changent ; les autres (payées, impayées) gardent leur montant. Les factures déjà émises ne changent pas.</li>
 * </ul>
 * Une année de cotisation reste la somme de ses échéances.
 */
final class CotisationsSuccessives {

    private final List<Avenant> acceptes;
    private final BigDecimal mensuelleActuelle;
    private final BigDecimal annuelleActuelle;
    private final boolean annuel;

    /**
     * @param acceptes avenants acceptés du contrat, par date d'effet croissante
     * @param mensuelleActuelle cotisation mensuelle TTC du contrat (après le dernier avenant)
     * @param annuelleActuelle cotisation annuelle TTC du contrat (après le dernier avenant)
     */
    CotisationsSuccessives(List<Avenant> acceptes, BigDecimal mensuelleActuelle, BigDecimal annuelleActuelle,
            boolean annuel) {
        this.acceptes = List.copyOf(acceptes);
        this.mensuelleActuelle = mensuelleActuelle;
        this.annuelleActuelle = annuelleActuelle;
        this.annuel = annuel;
    }

    /** Les lignes, avec le montant en vigueur à leur date ; {@code statutsEnBase} : statut des échéances enregistrées. */
    List<LigneEcheance> appliquer(List<LigneEcheance> lignes, Map<LocalDate, String> statutsEnBase) {
        if (acceptes.isEmpty()) {
            return lignes;
        }
        LocalDate premierEffet = acceptes.getFirst().getDateEffet();
        return lignes.stream().map(ligne -> {
            String statutEnBase = statutsEnBase.get(ligne.date());
            boolean garde = statutEnBase != null
                    && (!EcheancierService.A_VENIR.equals(statutEnBase) || ligne.date().isBefore(premierEffet));
            return garde ? ligne : new LigneEcheance(ligne.date(), enVigueur(ligne.date()), ligne.statut(),
                    ligne.periodeDebut(), ligne.periodeFin());
        }).toList();
    }

    /** Cotisation de l'échéance à cette date : l'ancienne d'un avenant qui prend effet après elle, sinon l'actuelle. */
    BigDecimal enVigueur(LocalDate date) {
        return acceptes.stream().filter(avenant -> avenant.getDateEffet().isAfter(date)).findFirst()
                .map(avenant -> annuel ? CalculAvenant.annuelle(avenant.getPrimeAvantTtc()) : avenant.getPrimeAvantTtc())
                .orElse(annuel ? annuelleActuelle : mensuelleActuelle);
    }
}
