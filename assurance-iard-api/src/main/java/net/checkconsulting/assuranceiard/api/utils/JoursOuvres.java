package net.checkconsulting.assuranceiard.api.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

/**
 * US-24, RG2 : jours ouvrés = tous les jours sauf les samedis, dimanches et jours fériés nationaux (métropole) : 1er
 * janvier, lundi de Pâques, 1er et 8 mai, Ascension, lundi de Pentecôte, 14 juillet, 15 août, 1er et 11 novembre,
 * 25 décembre. Les jours fériés propres à l'Alsace-Moselle et aux outre-mer ne sont pas comptés (à valider PO).
 */
public final class JoursOuvres {

    private static final Set<DayOfWeek> WEEK_END = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    private static final Set<MonthDay> FERIES_FIXES = Set.of(MonthDay.of(1, 1), MonthDay.of(5, 1), MonthDay.of(5, 8),
            MonthDay.of(7, 14), MonthDay.of(8, 15), MonthDay.of(11, 1), MonthDay.of(11, 11), MonthDay.of(12, 25));
    private static final int LUNDI_DE_PAQUES = 1;
    private static final int ASCENSION = 39;
    private static final int LUNDI_DE_PENTECOTE = 50;

    private JoursOuvres() {
    }

    /** Le jour ouvré atteint après {@code nombre} jours ouvrés comptés à partir du lendemain de {@code depart}. */
    public static LocalDate ajouter(LocalDate depart, int nombre) {
        LocalDate jour = depart;
        int restants = nombre;
        while (restants > 0) {
            jour = jour.plusDays(1);
            if (estOuvre(jour)) {
                restants--;
            }
        }
        return jour;
    }

    public static boolean estOuvre(LocalDate jour) {
        return !WEEK_END.contains(jour.getDayOfWeek()) && !estFerie(jour);
    }

    public static boolean estFerie(LocalDate jour) {
        if (FERIES_FIXES.contains(MonthDay.from(jour))) {
            return true;
        }
        LocalDate paques = paques(jour.getYear());
        return jour.equals(paques.plusDays(LUNDI_DE_PAQUES)) || jour.equals(paques.plusDays(ASCENSION))
                || jour.equals(paques.plusDays(LUNDI_DE_PENTECOTE));
    }

    /** Dimanche de Pâques (calendrier grégorien, algorithme de Meeus / Jones / Butcher). */
    static LocalDate paques(int annee) {
        int a = annee % 19;
        int b = annee / 100;
        int c = annee % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mois = (h + l - 7 * m + 114) / 31;
        int jour = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(annee, mois, jour);
    }
}
