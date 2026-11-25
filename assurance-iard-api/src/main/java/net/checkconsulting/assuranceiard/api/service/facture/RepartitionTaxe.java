package net.checkconsulting.assuranceiard.api.service.facture;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * US-22, AC3 et RG1 : répartit une prime TTC entre les garanties (au prorata de leur prix mensuel TTC), puis calcule
 * pour chaque garantie la prime hors taxe et la TSCA à partir du TTC. Les arrondis sont ajustés pour que, au centime
 * près, la somme des lignes TTC = la prime TTC et, sur chaque ligne, HT + taxe = TTC (le total aussi).
 */
public final class RepartitionTaxe {

    private static final int CENTIMES = 2;

    private RepartitionTaxe() {
    }

    /**
     * Une garantie de la facture.
     *
     * @param libelle libellé imprimé.
     * @param poids prix mensuel TTC (sert seulement de clé de répartition) ; zéro ou négatif = rien à facturer.
     * @param taux taux de TSCA (0.09 pour 9 %).
     */
    public record Garantie(String libelle, BigDecimal poids, BigDecimal taux) {
    }

    /** Une ligne de la facture : HT + taxe = TTC. */
    public record Ligne(String libelle, BigDecimal taux, BigDecimal montantHt, BigDecimal taxe, BigDecimal montantTtc) {
    }

    /** Les lignes et leurs totaux (HT + taxe = TTC). */
    public record Detail(List<Ligne> lignes, BigDecimal montantHt, BigDecimal taxe, BigDecimal montantTtc) {

        public Detail {
            lignes = List.copyOf(lignes);
        }
    }

    /**
     * @param montantTtc prime TTC à répartir (2 décimales).
     * @param garanties garanties, la formule en premier ; au moins une.
     */
    public static Detail repartir(BigDecimal montantTtc, List<Garantie> garanties) {
        if (garanties.isEmpty()) {
            throw new IllegalArgumentException("Au moins une garantie");
        }
        BigDecimal total = montantTtc.setScale(CENTIMES, RoundingMode.HALF_UP);
        List<BigDecimal> parts = parts(total, garanties);
        List<Ligne> lignes = new ArrayList<>();
        BigDecimal ht = BigDecimal.ZERO.setScale(CENTIMES);
        BigDecimal taxe = BigDecimal.ZERO.setScale(CENTIMES);
        for (int i = 0; i < garanties.size(); i++) {
            BigDecimal ttc = parts.get(i);
            // Une garantie sans part n'est pas imprimée (sauf si la prime est nulle : la formule seule).
            if (ttc.signum() == 0 && (total.signum() != 0 || i > 0)) {
                continue;
            }
            Garantie garantie = garanties.get(i);
            BigDecimal ligneHt = ttc.divide(BigDecimal.ONE.add(garantie.taux()), CENTIMES, RoundingMode.HALF_UP);
            BigDecimal ligneTaxe = ttc.subtract(ligneHt);
            lignes.add(new Ligne(garantie.libelle(), garantie.taux(), ligneHt, ligneTaxe, ttc));
            ht = ht.add(ligneHt);
            taxe = taxe.add(ligneTaxe);
        }
        return new Detail(lignes, ht, taxe, total);
    }

    /** Parts TTC au prorata des poids, au centime ; les centimes restants vont aux plus grands restes. */
    private static List<BigDecimal> parts(BigDecimal total, List<Garantie> garanties) {
        List<BigDecimal> poids = garanties.stream()
                .map(garantie -> garantie.poids() == null || garantie.poids().signum() < 0 ? BigDecimal.ZERO
                        : garantie.poids())
                .toList();
        BigDecimal somme = poids.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalCentimes = total.movePointRight(CENTIMES).longValueExact();
        long[] centimes = new long[garanties.size()];
        if (somme.signum() == 0) {
            // Aucun prix connu : tout sur la première garantie (la formule).
            centimes[0] = totalCentimes;
        } else {
            BigDecimal[] restes = new BigDecimal[garanties.size()];
            long reparti = 0;
            for (int i = 0; i < centimes.length; i++) {
                BigDecimal exact = poids.get(i).multiply(BigDecimal.valueOf(totalCentimes))
                        .divide(somme, 10, RoundingMode.HALF_UP);
                centimes[i] = exact.setScale(0, RoundingMode.FLOOR).longValueExact();
                restes[i] = exact.subtract(BigDecimal.valueOf(centimes[i]));
                reparti += centimes[i];
            }
            long manquants = totalCentimes - reparti;
            List<Integer> ordre = IntStream.range(0, centimes.length).boxed()
                    .sorted(Comparator.comparing((Integer i) -> restes[i]).reversed()
                            .thenComparing(Comparator.naturalOrder()))
                    .toList();
            for (int k = 0; k < manquants; k++) {
                centimes[ordre.get(k % ordre.size())]++;
            }
        }
        return IntStream.range(0, centimes.length)
                .mapToObj(i -> BigDecimal.valueOf(centimes[i], CENTIMES))
                .toList();
    }
}
