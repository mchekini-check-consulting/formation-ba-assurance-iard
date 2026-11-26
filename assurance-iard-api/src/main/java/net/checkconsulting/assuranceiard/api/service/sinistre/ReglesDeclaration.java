package net.checkconsulting.assuranceiard.api.service.sinistre;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.repository.ReferentielSinistresRepository.TypeSinistre;
import net.checkconsulting.assuranceiard.api.utils.JoursOuvres;

/**
 * US-24 : règles de la déclaration, sans accès aux données (décisions BA, à valider PO).
 * <ul>
 *   <li>AC1 : contrats ouverts à la déclaration ; un contrat résilié l'est encore 2 ans après sa date de fin.</li>
 *   <li>AC3, RG4 : un type est couvert si la formule ou une option en vigueur à la date du sinistre le couvre ;
 *       jamais de message pour « Autre » (RG11).</li>
 *   <li>AC5 : date antérieure à la date d'effet ou postérieure à la date de fin.</li>
 *   <li>AC6, RG2 : 5 jours ouvrés, 2 pour un vol ; catastrophe naturelle : 30 jours après la publication de
 *       l'arrêté, rien si sa date est inconnue. Jamais bloquant (RG3).</li>
 * </ul>
 */
public final class ReglesDeclaration {

    static final String RESILIE = "RESILIE";
    /** AC1 : statuts qui permettent de déclarer (RESILIE : moins de 2 ans après la date de fin). */
    public static final Set<String> STATUTS_DECLARABLES = Set.of("ACTIF", "ACTIF_JUSTIFICATIFS_ATTENDUS",
            "AVENANT_EN_ATTENTE", "IMPAYE", "SUSPENDU", "RESILIATION_EN_COURS", RESILIE);
    static final int ANNEES_APRES_RESILIATION = 2;

    private ReglesDeclaration() {
    }

    public static boolean declarable(Contrat contrat, LocalDate aujourdhui) {
        if (!STATUTS_DECLARABLES.contains(contrat.getStatut())) {
            return false;
        }
        if (!RESILIE.equals(contrat.getStatut())) {
            return true;
        }
        return contrat.getDateFin() != null
                && contrat.getDateFin().isAfter(aujourdhui.minusYears(ANNEES_APRES_RESILIATION));
    }

    /**
     * AC3 : vide pour « Autre » (aucun message), sinon vrai si la formule ou une des options en vigueur à la date du
     * sinistre couvre ce type. Les codes sont comparés sans tenir compte des majuscules.
     */
    public static Optional<Boolean> couvert(TypeSinistre type, String formule, Collection<String> options) {
        if (type.autre()) {
            return Optional.empty();
        }
        boolean parFormule = formule != null && contient(type.formulesCouvrantes(), formule);
        boolean parOption = options.stream().anyMatch(option -> contient(type.optionsCouvrantes(), option));
        return Optional.of(parFormule || parOption);
    }

    /** AC5 : la date du sinistre est hors de la période du contrat. */
    public static boolean horsPeriode(Contrat contrat, LocalDate dateSurvenance) {
        if (dateSurvenance == null) {
            return false;
        }
        boolean avant = contrat.getDateEffet() != null && dateSurvenance.isBefore(contrat.getDateEffet());
        boolean apres = contrat.getDateFin() != null && dateSurvenance.isAfter(contrat.getDateFin());
        return avant || apres;
    }

    /** RG2 : dernier jour pour déclarer ; vide si on ne peut pas le calculer (catastrophe naturelle sans arrêté). */
    public static Optional<LocalDate> dateLimite(TypeSinistre type, LocalDate dateSurvenance, LocalDate dateArrete) {
        if (type.delaiJoursArrete() != null) {
            return Optional.ofNullable(dateArrete).map(arrete -> arrete.plusDays(type.delaiJoursArrete()));
        }
        if (dateSurvenance == null || type.delaiJoursOuvres() == null) {
            return Optional.empty();
        }
        return Optional.of(JoursOuvres.ajouter(dateSurvenance, type.delaiJoursOuvres()));
    }

    /** AC6 : déclaration faite après la date limite (jamais bloquant). */
    public static boolean horsDelai(TypeSinistre type, LocalDate dateSurvenance, LocalDate dateArrete,
            LocalDate aujourdhui) {
        return dateLimite(type, dateSurvenance, dateArrete).map(aujourdhui::isAfter).orElse(false);
    }

    /** AC6 : le délai prévu, en toutes lettres (« 5 jours ouvrés »). */
    public static String delaiPrevu(TypeSinistre type) {
        if (type.delaiJoursArrete() != null) {
            return type.delaiJoursArrete() + " jours après la publication de l'arrêté de catastrophe naturelle";
        }
        return type.delaiJoursOuvres() == null ? null : type.delaiJoursOuvres() + " jours ouvrés";
    }

    private static boolean contient(Collection<String> codes, String code) {
        String cherche = code.toLowerCase(Locale.ROOT);
        return codes.stream().anyMatch(candidat -> candidat.toLowerCase(Locale.ROOT).equals(cherche));
    }
}
