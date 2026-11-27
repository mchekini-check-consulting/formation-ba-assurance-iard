package net.checkconsulting.moteurtarifaire.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaireAuto;
import net.checkconsulting.moteurtarifaire.api.dto.TarifAutoRequest;
import net.checkconsulting.moteurtarifaire.api.dto.TarifAutoResponse;
import net.checkconsulting.moteurtarifaire.api.dto.TarifAutoResponse.Detail;
import net.checkconsulting.moteurtarifaire.api.dto.TarifAutoResponse.PrixFormule;
import net.checkconsulting.moteurtarifaire.api.dto.TarifAutoResponse.PrixOption;
import net.checkconsulting.moteurtarifaire.api.enums.FormuleAuto;
import net.checkconsulting.moteurtarifaire.api.enums.UsageAuto;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleIndisponibleException;
import net.checkconsulting.moteurtarifaire.api.logging.LogMetier;
import net.checkconsulting.moteurtarifaire.api.model.RefFormule;
import net.checkconsulting.moteurtarifaire.api.model.RefTarif;
import org.springframework.stereotype.Service;

/** US-29 V9 : options auto avec codes corrects (ASSISTANCE_0KM, VEHICULE_REMPLACEMENT, PROTECTION_CONDUCTEUR). */
@Slf4j
@Service
@RequiredArgsConstructor
public class TarificateurAuto {

    private static final BigDecimal MOIS_PAR_AN = BigDecimal.valueOf(12);
    private static final int SEUIL_ANNEES_NOVICE = 3;

    private final GrilleTarifaireAutoService grilles;
    private final Clock horloge;

    @LogMetier(champs = {"demande.usage", "demande.stationnement", "resultat.tarifable", "resultat.formuleConseillee"})
    public TarifAutoResponse calculer(TarifAutoRequest demande) {
        LocalDate aujourdhui = LocalDate.now(horloge);
        GrilleTarifaireAuto grille = grilles.applicableAu(aujourdhui)
                .orElseThrow(() -> new GrilleIndisponibleException(aujourdhui));

        int anneesPermis = aujourdhui.getYear() - demande.anneePermis();
        boolean novice = anneesPermis < SEUIL_ANNEES_NOVICE;

        BigDecimal coeff = calculerCoefficient(grille, demande, anneesPermis, novice);

        List<PrixFormule> prix = grille.formules().stream()
                .map(f -> prixFormule(f, coeff))
                .toList();

        List<PrixOption> prixOptions = grille.options().stream()
                .map(o -> new PrixOption(o.getCode(), o.getPrixMensuel()))
                .toList();

        String formuleConseillee = conseillerFormule(demande.valeurVehicule(), demande.kmAnnuels(), demande.usage());

        TarifAutoResponse.TarifAutoResponseBuilder reponse = TarifAutoResponse.builder()
                .idTarif(UUID.randomUUID())
                .versionGrille(grille.version())
                .tarifable(true)
                .formuleConseillee(formuleConseillee)
                .prix(prix)
                .prixOptions(prixOptions)
                .dateValidite(aujourdhui.plusDays(grille.dureeValiditeJours()));

        if (demande.formule() != null) {
            reponse.detail(calculerDetail(demande, grille, coeff));
        }

        TarifAutoResponse resultat = reponse.build();
        log.info("Calcul tarifaire AUTO : grille={}, formuleConseillee={}", grille.version(), formuleConseillee);
        return resultat;
    }

    private BigDecimal calculerCoefficient(GrilleTarifaireAuto grille, TarifAutoRequest demande,
            int anneesPermis, boolean novice) {
        BigDecimal coeffAge = coefficient(grille, "AGE", BigDecimal.valueOf(anneesPermis), null);
        BigDecimal coeffBonusMalus = coefficient(grille, "BONUS_MALUS", demande.bonusMalus(), null);
        BigDecimal coeffUsage = coefficient(grille, "USAGE", null, demande.usage().name());
        BigDecimal coeffVehicule = coefficient(grille, "VALEUR_VEHICULE", demande.valeurVehicule(), null);
        BigDecimal coeffKm = coefficient(grille, "KM_ANNUEL", BigDecimal.valueOf(demande.kmAnnuels()), null);
        BigDecimal coeffStat = coefficient(grille, "STATIONNEMENT", null, demande.stationnement().name());
        BigDecimal coeffNovice = coefficient(grille, "SURPRIME_NOVICE", null, novice ? "OUI" : "NON");

        return coeffAge
                .multiply(coeffBonusMalus)
                .multiply(coeffUsage)
                .multiply(coeffVehicule)
                .multiply(coeffKm)
                .multiply(coeffStat)
                .multiply(coeffNovice);
    }

    private BigDecimal coefficient(GrilleTarifaireAuto grille, String critere,
            BigDecimal valeurNumerique, String valeurTexte) {
        List<RefTarif> lignes = grille.coefficientsParCritere().getOrDefault(critere, List.of());
        Optional<RefTarif> ligne;
        if (valeurTexte != null) {
            ligne = lignes.stream()
                    .filter(t -> valeurTexte.equals(t.getValeur()))
                    .findFirst();
        } else {
            ligne = lignes.stream()
                    .filter(t -> estDansLaTranche(valeurNumerique, t.getBorneMin(), t.getBorneMax()))
                    .findFirst();
        }
        if (ligne.isEmpty()) {
            log.warn("Coefficient introuvable pour critère={}, valeur={}", critere,
                    valeurTexte != null ? valeurTexte : valeurNumerique);
            return BigDecimal.ONE;
        }
        return ligne.get().getCoefficient();
    }

    private boolean estDansLaTranche(BigDecimal valeur, BigDecimal min, BigDecimal max) {
        if (min != null && valeur.compareTo(min) < 0) return false;
        if (max != null && valeur.compareTo(max) > 0) return false;
        return true;
    }

    private PrixFormule prixFormule(RefFormule formule, BigDecimal coeff) {
        BigDecimal mensuel = formule.getPrixMensuelBase().multiply(coeff)
                .setScale(2, RoundingMode.HALF_UP);
        return new PrixFormule(formule.getCode(), mensuel, mensuel.multiply(MOIS_PAR_AN));
    }

    private Detail calculerDetail(TarifAutoRequest demande, GrilleTarifaireAuto grille, BigDecimal coeff) {
        String codeFormule = demande.formule().name();
        RefFormule formule = grille.formules().stream()
                .filter(f -> codeFormule.equals(f.getCode()))
                .findFirst()
                .orElseThrow();

        BigDecimal prixFormuleBase = formule.getPrixMensuelBase().multiply(coeff)
                .setScale(2, RoundingMode.HALF_UP);

        List<String> codesOptions = demande.options() != null ? demande.options() : List.of();
        List<PrixOption> lignesOptions = grille.options().stream()
                .filter(o -> codesOptions.contains(o.getCode()))
                .filter(o -> o.getFormulesCompatibles().contains(codeFormule))
                .map(o -> new PrixOption(o.getCode(), o.getPrixMensuel()))
                .toList();

        BigDecimal totalOptions = lignesOptions.stream()
                .map(PrixOption::prixMensuel)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMensuel = prixFormuleBase.add(totalOptions).setScale(2, RoundingMode.HALF_UP);

        return new Detail(codeFormule, prixFormuleBase, lignesOptions, totalMensuel,
                totalMensuel.multiply(MOIS_PAR_AN));
    }

    private String conseillerFormule(BigDecimal valeurVehicule, int kmAnnuels, UsageAuto usage) {
        if (valeurVehicule.compareTo(BigDecimal.valueOf(25000)) > 0) {
            return FormuleAuto.TR.name();
        }
        if (valeurVehicule.compareTo(BigDecimal.valueOf(10000)) > 0
                || kmAnnuels > 15000
                || usage == UsageAuto.PROFESSIONNEL) {
            return FormuleAuto.INTER.name();
        }
        return FormuleAuto.TIERS.name();
    }
}
