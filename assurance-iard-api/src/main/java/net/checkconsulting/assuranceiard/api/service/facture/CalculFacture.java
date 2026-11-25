package net.checkconsulting.assuranceiard.api.service.facture;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.config.FacturationProperties;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.service.document.MiseEnForme;
import net.checkconsulting.assuranceiard.api.service.facture.RepartitionTaxe.Garantie;
import org.springframework.stereotype.Component;

/**
 * US-22, AC3 et RG1 : détail d'une facture par garantie. La prime connue est TTC : elle est répartie entre la formule
 * (garanties de base) et chaque option au prix connu, au prorata de leur prix mensuel TTC ; chaque part est
 * décomposée en HT et TSCA avec le taux réglé dans application.yaml (assurance.facturation).
 */
@Component
@RequiredArgsConstructor
public class CalculFacture {

    private final FacturationProperties reglages;

    public RepartitionTaxe.Detail detail(DonneesContrat contrat, BigDecimal montantTtc) {
        return RepartitionTaxe.repartir(montantTtc, garanties(contrat));
    }

    /** La formule en premier (son poids = cotisation mensuelle moins les options au prix connu), puis les options. */
    List<Garantie> garanties(DonneesContrat contrat) {
        String produit = contrat.produit();
        List<DonneesContrat.Option> avecPrix = contrat.options().stream()
                .filter(option -> option.prixMensuelTtc() != null).toList();
        BigDecimal options = avecPrix.stream().map(DonneesContrat.Option::prixMensuelTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mensuelle = Objects.requireNonNullElse(contrat.tarif().cotisationMensuelle(), BigDecimal.ZERO);
        List<Garantie> garanties = new ArrayList<>();
        garanties.add(new Garantie("Formule " + MiseEnForme.formule(contrat.formule()) + " (garanties de base)",
                mensuelle.subtract(options), reglages.tauxDuProduit(produit)));
        avecPrix.forEach(option -> garanties.add(new Garantie(MiseEnForme.option(option.code()),
                option.prixMensuelTtc(), reglages.tauxDeLOption(produit, option.code()))));
        return garanties;
    }
}
