package net.checkconsulting.assuranceiard.api.service.avenant;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.checkconsulting.assuranceiard.api.exception.AvenantInvalideException;

/**
 * US-23, AC3 : ce que change l'avenant. Nouvelle cotisation mensuelle TTC = ancienne + prix des garanties ajoutées
 * − prix des garanties retirées, avec les prix affichés à l'écran ({@link CatalogueGaranties}) : la formule de base
 * n'est pas recalculée (hypothèse à valider PO). Cotisation annuelle = 12 mensualités.
 */
public final class CalculAvenant {

    public static final String AJOUT = "AJOUT";
    public static final String RETRAIT = "RETRAIT";
    public static final BigDecimal MOIS_PAR_AN = BigDecimal.valueOf(12);

    private CalculAvenant() {
    }

    /** Une garantie ajoutée ou retirée. */
    public record Ligne(String code, String libelle, String action, BigDecimal prixMensuelTtc) {
    }

    /** Garanties ajoutées et retirées, cotisations mensuelles TTC avant et après. */
    public record Changement(List<Ligne> ajouts, List<Ligne> retraits, BigDecimal primeAvant, BigDecimal primeApres) {

        public Changement {
            ajouts = List.copyOf(ajouts);
            retraits = List.copyOf(retraits);
        }

        /** RG4 : la couverture diminue dès qu'une garantie est retirée. */
        public boolean reduitLaCouverture() {
            return !retraits.isEmpty();
        }

        public List<Ligne> lignes() {
            List<Ligne> toutes = new ArrayList<>(ajouts);
            toutes.addAll(retraits);
            return toutes;
        }
    }

    /**
     * @param garanties garanties proposées (souscrites et ajoutables)
     * @param souhaitees codes des garanties voulues après l'avenant
     * @param primeAvant cotisation mensuelle TTC actuelle
     * @throws AvenantInvalideException garantie inconnue, garantie non modifiable changée, ou aucun changement
     */
    public static Changement calculer(List<GarantieProposee> garanties, Collection<String> souhaitees,
            BigDecimal primeAvant) {
        Set<String> voulues = new LinkedHashSet<>(souhaitees);
        if (!garanties.stream().map(GarantieProposee::code).toList().containsAll(voulues)) {
            throw new AvenantInvalideException(AvenantInvalideException.GARANTIE_INCONNUE);
        }
        List<Ligne> lignes = garanties.stream()
                .filter(garantie -> voulues.contains(garantie.code()) != garantie.souscrite())
                .map(garantie -> ligne(garantie, voulues.contains(garantie.code())))
                .toList();
        if (lignes.isEmpty()) {
            throw new AvenantInvalideException(AvenantInvalideException.AUCUN_CHANGEMENT);
        }
        List<Ligne> ajouts = lignes.stream().filter(ligne -> AJOUT.equals(ligne.action())).toList();
        List<Ligne> retraits = lignes.stream().filter(ligne -> RETRAIT.equals(ligne.action())).toList();
        BigDecimal apres = primeAvant.add(somme(ajouts)).subtract(somme(retraits));
        return new Changement(ajouts, retraits, arrondi(primeAvant), arrondi(apres));
    }

    /** AC2 : une garantie non modifiable (obligatoire) ne s'ajoute ni ne se retire. */
    private static Ligne ligne(GarantieProposee garantie, boolean ajout) {
        if (!garantie.modifiable()) {
            throw new AvenantInvalideException(garantie.raison());
        }
        return new Ligne(garantie.code(), garantie.libelle(), ajout ? AJOUT : RETRAIT, garantie.prixMensuelTtc());
    }

    /** Cotisation annuelle TTC d'une cotisation mensuelle (12 mensualités). */
    public static BigDecimal annuelle(BigDecimal mensuelle) {
        return arrondi(mensuelle.multiply(MOIS_PAR_AN));
    }

    private static BigDecimal somme(List<Ligne> lignes) {
        return lignes.stream().map(Ligne::prixMensuelTtc).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal arrondi(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP);
    }
}
