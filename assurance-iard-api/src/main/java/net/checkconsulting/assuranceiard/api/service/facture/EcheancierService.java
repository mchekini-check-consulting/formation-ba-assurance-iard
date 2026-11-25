package net.checkconsulting.assuranceiard.api.service.facture;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.model.Echeance;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.repository.EcheanceRepository;
import net.checkconsulting.assuranceiard.api.service.PremierPrelevementService;
import org.springframework.stereotype.Service;

/**
 * US-22, AC1 : échéancier d'un contrat et ses années de cotisation (années d'assurance, d'une date anniversaire de
 * l'effet à la suivante).
 * <ul>
 *   <li>Si des échéances sont enregistrées (table echeance, données de test T-05), elles priment.</li>
 *   <li>Sinon (aucun traitement ne les crée encore), elles sont calculées depuis le contrat : premier prélèvement
 *       (RG3/RG4 d'US-14 : jour choisi, au moins 14 jours après la signature), puis chaque mois (ou chaque année en
 *       paiement annuel) jusqu'à la fin de l'année d'assurance en cours, sans dépasser la date de fin.</li>
 *   <li>Statut d'une échéance calculée : « Payée » si sa date est passée, « À venir » sinon. Simulation de prototype,
 *       À VALIDER PO : il n'y a pas encore de système de paiement.</li>
 * </ul>
 * Aucun échéancier pour un contrat en attente de signature.
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

    /** Une année d'assurance qui a au moins une échéance ; montant = somme de ses échéances. */
    public record AnneeCotisation(LocalDate debut, LocalDate fin, BigDecimal montantTtc) {
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
        List<Echeance> enBase = echeances.findByContratIdOrderByDateEcheanceDescIdDesc(contrat.id());
        List<LigneEcheance> lignes = new ArrayList<>(enBase.isEmpty() ? calculees(contrat, annuel)
                : enBase.stream().map(echeance -> ligne(echeance.getDateEcheance(), echeance.getMontantTtc(),
                        echeance.getStatut(), annuel, tarif.dateFin())).toList());
        lignes.sort(Comparator.comparing(LigneEcheance::date).reversed());
        return new Echeancier(lignes, annees(lignes, tarif.dateEffet(), tarif.dateFin()));
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
        lignes.forEach(ligne -> parAnnee.merge(debutAnnee(effet, ligne.date()), ligne.montantTtc(), BigDecimal::add));
        return parAnnee.entrySet().stream()
                .map(annee -> new AnneeCotisation(annee.getKey(),
                        plafond(annee.getKey().plusYears(1).minusDays(1), finContrat), annee.getValue()))
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
