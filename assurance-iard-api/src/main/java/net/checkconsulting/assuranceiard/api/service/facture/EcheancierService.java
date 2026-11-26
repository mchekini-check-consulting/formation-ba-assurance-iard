package net.checkconsulting.assuranceiard.api.service.facture;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.model.Avenant;
import net.checkconsulting.assuranceiard.api.model.Echeance;
import net.checkconsulting.assuranceiard.api.repository.AvenantRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.repository.EcheanceRepository;
import net.checkconsulting.assuranceiard.api.service.PremierPrelevementService;
import org.springframework.stereotype.Service;

/**
 * US-22, AC1 : échéancier d'un contrat et ses années de cotisation (années d'assurance, d'une date anniversaire de
 * l'effet à la suivante).
 * <ul>
 *   <li>Si des échéances sont enregistrées (table echeance, données de test T-05), elles priment ; les mois manquants
 *       de leurs années d'assurance sont complétés par les échéances calculées (recette US-22 : une année compte
 *       toutes ses mensualités).</li>
 *   <li>Sinon (aucun traitement ne les crée encore), elles sont calculées depuis le contrat : premier prélèvement
 *       (RG3/RG4 d'US-14 : jour choisi, au moins 14 jours après la signature), puis chaque mois (ou chaque année en
 *       paiement annuel) jusqu'à la fin de l'année d'assurance en cours, sans dépasser la date de fin.</li>
 *   <li>Statut d'une échéance calculée : « Payée » si sa date est passée, « À venir » sinon. Simulation de prototype,
 *       À VALIDER PO : il n'y a pas encore de système de paiement.</li>
 * </ul>
 * Aucun échéancier pour un contrat en attente de signature. Recette : aucune échéance avant la date d'effet (une
 * échéance enregistrée avant est ignorée) ; l'échéancier et les années de cotisation commencent à la date d'effet.
 */
@Service
@RequiredArgsConstructor
public class EcheancierService {

    public static final String EN_ATTENTE_DE_SIGNATURE = "EN_ATTENTE_DE_SIGNATURE";
    public static final String PAYEE = "PAYEE";
    public static final String A_VENIR = "A_VENIR";
    static final String ANNUEL = "ANNUEL";
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");

    private final EcheanceRepository echeances;
    private final AvenantRepository avenants;
    private final DonneesContratRepository donneesContrat;
    private final PremierPrelevementService premierPrelevement;
    private final Clock horloge;

    /**
     * Une échéance et la période qu'elle couvre (un mois, ou un an en paiement annuel, sans dépasser la fin du
     * contrat).
     *
     * @param statut PAYEE, A_VENIR ou IMPAYEE.
     */
    public record LigneEcheance(LocalDate date, BigDecimal montantTtc, String statut, LocalDate periodeDebut,
            LocalDate periodeFin) {
    }

    /**
     * Une année d'assurance qui a au moins une échéance ; montant = somme de toutes ses échéances (12 mensualités).
     *
     * @param reglee vrai si toutes ses échéances sont payées (recette US-22 : seule une année réglée se facture).
     */
    public record AnneeCotisation(LocalDate debut, LocalDate fin, BigDecimal montantTtc, boolean reglee) {
    }

    /** Échéances et années, de la plus récente à la plus ancienne. */
    public record Echeancier(List<LigneEcheance> echeances, List<AnneeCotisation> annees) {

        public static final Echeancier VIDE = new Echeancier(List.of(), List.of());

        public Echeancier {
            echeances = List.copyOf(echeances);
            annees = List.copyOf(annees);
        }

        public boolean estVide() {
            return echeances.isEmpty();
        }
    }

    /** L'échéancier du contrat dans ce statut. */
    public Echeancier de(String statut, DonneesContrat contrat) {
        DonneesContrat.Tarif tarif = contrat.tarif();
        if (EN_ATTENTE_DE_SIGNATURE.equals(statut) || tarif.dateEffet() == null) {
            return Echeancier.VIDE;
        }
        boolean annuel = ANNUEL.equals(contrat.paiement().frequence());
        // Recette US-22 : aucune échéance avant la date d'effet, même enregistrée.
        List<Echeance> enBase = echeances.findByContratIdOrderByDateEcheanceDescIdDesc(contrat.id()).stream()
                .filter(echeance -> !echeance.getDateEcheance().isBefore(tarif.dateEffet()))
                .toList();
        List<LigneEcheance> lignes = new ArrayList<>(selonLesAvenants(contrat, annuel, enBase,
                enBase.isEmpty() ? calculees(contrat, annuel) : completees(contrat, annuel, enBase)));
        lignes.removeIf(ligne -> ligne.date().isBefore(tarif.dateEffet()));
        lignes.sort(Comparator.comparing(LigneEcheance::date).reversed());
        return new Echeancier(lignes, annees(lignes, tarif.dateEffet(), tarif.dateFin()));
    }

    /** US-23, RG3 : montants des échéances après des avenants acceptés ({@link CotisationsSuccessives}). */
    private List<LigneEcheance> selonLesAvenants(DonneesContrat contrat, boolean annuel, List<Echeance> enBase,
            List<LigneEcheance> lignes) {
        List<Avenant> acceptes = avenants.findByContratIdAndStatutOrderByDateEffetAscIdAsc(contrat.id(),
                AvenantRepository.ACCEPTE);
        Map<LocalDate, String> statutsEnBase = new HashMap<>();
        enBase.forEach(echeance -> statutsEnBase.putIfAbsent(echeance.getDateEcheance(), echeance.getStatut()));
        return new CotisationsSuccessives(acceptes, contrat.tarif().cotisationMensuelle(),
                contrat.tarif().cotisationAnnuelle(), annuel).appliquer(lignes, statutsEnBase);
    }

    /**
     * Recette US-22 : les échéances enregistrées, complétées par les échéances calculées des mois manquants de leurs
     * années d'assurance (une année de cotisation compte toutes ses mensualités, pas seulement celles en base).
     */
    private List<LigneEcheance> completees(DonneesContrat contrat, boolean annuel, List<Echeance> enBase) {
        LocalDate effet = contrat.tarif().dateEffet();
        LocalDate fin = contrat.tarif().dateFin();
        List<LigneEcheance> lignes = new ArrayList<>(enBase.stream()
                .map(echeance -> ligne(echeance.getDateEcheance(), echeance.getMontantTtc(), echeance.getStatut(),
                        annuel, fin))
                .toList());
        BigDecimal mensuelle = contrat.tarif().cotisationMensuelle();
        if (annuel || mensuelle == null) {
            return lignes;
        }
        Set<YearMonth> moisEnBase = lignes.stream().map(ligne -> YearMonth.from(ligne.date()))
                .collect(Collectors.toSet());
        Set<LocalDate> anneesEnBase = lignes.stream().map(ligne -> debutAnnee(effet, ligne.date()))
                .collect(Collectors.toCollection(TreeSet::new));
        for (LocalDate debut : anneesEnBase) {
            int jour = jourDePrelevement(contrat, lignes, effet, debut);
            completerAnnee(debut, jour, mensuelle, fin, moisEnBase, lignes);
        }
        return lignes;
    }

    /** Jour de prélèvement du contrat, à défaut celui des échéances enregistrées de cette année d'assurance. */
    private static int jourDePrelevement(DonneesContrat contrat, List<LigneEcheance> lignes, LocalDate effet,
            LocalDate debutAnnee) {
        Integer jour = contrat.paiement().jourPrelevement();
        if (jour != null) {
            return jour;
        }
        return lignes.stream().filter(ligne -> debutAnnee(effet, ligne.date()).equals(debutAnnee))
                .findFirst().orElseThrow().date().getDayOfMonth();
    }

    /**
     * Ajoute à {@code lignes} les échéances calculées des mois de cette année d'assurance qui n'en ont pas encore
     * (le mois est alors ajouté à {@code moisEnBase}) : une par mois, ce jour-là, sans dépasser la fin de l'année ni
     * celle du contrat.
     */
    private void completerAnnee(LocalDate debutAnnee, int jour, BigDecimal mensuelle, LocalDate fin,
            Set<YearMonth> moisEnBase, List<LigneEcheance> lignes) {
        LocalDate aujourdhui = aujourdhui();
        LocalDate finAnnee = debutAnnee.plusYears(1).minusDays(1);
        LocalDate premiere = jourDuMois(debutAnnee, jour);
        if (premiere.isBefore(debutAnnee)) {
            premiere = jourDuMois(debutAnnee.plusMonths(1), jour);
        }
        int rang = 0;
        LocalDate date = premiere;
        while (avantLesFins(date, finAnnee, fin)) {
            if (moisEnBase.add(YearMonth.from(date))) {
                lignes.add(ligne(date, mensuelle, date.isBefore(aujourdhui) ? PAYEE : A_VENIR, false, fin));
            }
            rang++;
            date = jourDuMois(premiere.plusMonths(rang), jour);
        }
    }

    /** Vrai si la date ne dépasse pas la fin de l'année d'assurance et tombe avant la fin du contrat (s'il en a une). */
    private static boolean avantLesFins(LocalDate date, LocalDate finAnnee, LocalDate finContrat) {
        return !date.isAfter(finAnnee) && (finContrat == null || date.isBefore(finContrat));
    }

    /** Ce jour du mois de la date (le dernier jour du mois s'il est plus court). */
    private static LocalDate jourDuMois(LocalDate date, int jour) {
        return date.withDayOfMonth(Math.min(jour, date.lengthOfMonth()));
    }

    private List<LigneEcheance> calculees(DonneesContrat contrat, boolean annuel) {
        DonneesContrat.Tarif tarif = contrat.tarif();
        BigDecimal montant = annuel ? tarif.cotisationAnnuelle() : tarif.cotisationMensuelle();
        if (montant == null) {
            return List.of();
        }
        LocalDate aujourdhui = aujourdhui();
        LocalDate effet = tarif.dateEffet();
        LocalDate fin = tarif.dateFin();
        LocalDate horizon = finAnnee(effet, aujourdhui.isAfter(effet) ? aujourdhui : effet);
        LocalDate premiere = premiere(contrat);
        List<LigneEcheance> lignes = new ArrayList<>();
        for (int rang = 0;; rang++) {
            LocalDate date = annuel ? premiere.plusYears(rang) : premiere.plusMonths(rang);
            if (date.isAfter(horizon) || fin != null && !date.isBefore(fin)) {
                break;
            }
            lignes.add(ligne(date, montant, date.isBefore(aujourdhui) ? PAYEE : A_VENIR, annuel, fin));
        }
        return lignes;
    }

    /** Premier prélèvement : RG3/RG4 d'US-14 si le contrat est signé, sinon le jour choisi à partir de l'effet. */
    private LocalDate premiere(DonneesContrat contrat) {
        LocalDate effet = contrat.tarif().dateEffet();
        Integer jour = contrat.paiement().jourPrelevement();
        if (jour == null) {
            return effet;
        }
        return donneesContrat.dateSignature(contrat.id())
                .map(signature -> premierPrelevement.calculer(effet, jour, signature))
                .orElseGet(() -> {
                    LocalDate candidate = effet.withDayOfMonth(jour);
                    return candidate.isBefore(effet) ? candidate.plusMonths(1) : candidate;
                });
    }

    private static LigneEcheance ligne(LocalDate date, BigDecimal montant, String statut, boolean annuel,
            LocalDate finContrat) {
        LocalDate fin = (annuel ? date.plusYears(1) : date.plusMonths(1)).minusDays(1);
        return new LigneEcheance(date, montant, statut, date, plafond(fin, finContrat));
    }

    private static List<AnneeCotisation> annees(List<LigneEcheance> lignes, LocalDate effet, LocalDate finContrat) {
        Map<LocalDate, BigDecimal> parAnnee = new LinkedHashMap<>();
        Map<LocalDate, Boolean> reglees = new LinkedHashMap<>();
        lignes.forEach(ligne -> {
            LocalDate debut = debutAnnee(effet, ligne.date());
            parAnnee.merge(debut, ligne.montantTtc(), BigDecimal::add);
            reglees.merge(debut, PAYEE.equals(ligne.statut()), Boolean::logicalAnd);
        });
        return parAnnee.entrySet().stream()
                .map(annee -> new AnneeCotisation(annee.getKey(),
                        plafond(annee.getKey().plusYears(1).minusDays(1), finContrat), annee.getValue(),
                        reglees.get(annee.getKey())))
                .sorted(Comparator.comparing(AnneeCotisation::debut).reversed())
                .toList();
    }

    /** Début de l'année d'assurance (anniversaire de l'effet) qui contient cette date. */
    static LocalDate debutAnnee(LocalDate effet, LocalDate date) {
        long annees = ChronoUnit.YEARS.between(effet, date);
        LocalDate debut = effet.plusYears(annees);
        if (debut.isAfter(date)) {
            debut = effet.plusYears(annees - 1);
        } else if (!effet.plusYears(annees + 1).isAfter(date)) {
            // Effet un 29 février : l'anniversaire tombe le 28 février, avant l'année pleine.
            debut = effet.plusYears(annees + 1);
        }
        return debut;
    }

    /** Dernier jour de l'année d'assurance qui contient cette date. */
    static LocalDate finAnnee(LocalDate effet, LocalDate date) {
        return debutAnnee(effet, date).plusYears(1).minusDays(1);
    }

    private static LocalDate plafond(LocalDate date, LocalDate finContrat) {
        return finContrat != null && date.isAfter(finContrat) ? finContrat : date;
    }

    private LocalDate aujourdhui() {
        return LocalDate.ofInstant(horloge.instant(), FRANCE);
    }
}
