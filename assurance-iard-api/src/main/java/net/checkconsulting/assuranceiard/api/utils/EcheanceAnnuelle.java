package net.checkconsulting.assuranceiard.api.utils;

import java.time.LocalDate;

/**
 * US-18, AC1 (hypothèse à valider PO) : l'échéance d'un contrat est sa date anniversaire annuelle, calculée depuis
 * la date d'effet. Un contrat qui prend fin avant cette date n'a plus d'échéance (sa date de fin est affichée).
 */
public final class EcheanceAnnuelle {

    private EcheanceAnnuelle() {
    }

    /**
     * Premier anniversaire de la date d'effet (au moins un an après l'effet) qui tombe aujourd'hui ou après. Un effet
     * au 29 février tombe le 28 février les années non bissextiles.
     *
     * @return null si la date d'effet est inconnue ou si le contrat se termine avant l'échéance
     */
    public static LocalDate prochaine(LocalDate dateEffet, LocalDate dateFin, LocalDate aujourdhui) {
        if (dateEffet == null) {
            return null;
        }
        long annees = 1;
        while (dateEffet.plusYears(annees).isBefore(aujourdhui)) {
            annees++;
        }
        LocalDate echeance = dateEffet.plusYears(annees);
        return dateFin != null && dateFin.isBefore(echeance) ? null : echeance;
    }
}
