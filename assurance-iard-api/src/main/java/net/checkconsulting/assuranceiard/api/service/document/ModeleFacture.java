package net.checkconsulting.assuranceiard.api.service.document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.Facture;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.repository.FactureRepository;
import net.checkconsulting.assuranceiard.api.service.document.ContenuDocument.Section;
import net.checkconsulting.assuranceiard.api.service.facture.CalculFacture;
import net.checkconsulting.assuranceiard.api.service.facture.RepartitionTaxe;
import org.springframework.stereotype.Component;

/**
 * US-22 : modèle « Facture » (point d'extension de T-03). Une facture par document : document.reference = numéro
 * de facture (à défaut, la facture qui pointe vers ce document, données de test T-05). AC3 : numéro, date, client,
 * contrat, période couverte, prime HT, TSCA par garantie, total TTC et mention d'exonération de TVA ; les mentions
 * de l'assureur, la charte et le pied de page sont ajoutés par le service commun.
 */
@Component
@RequiredArgsConstructor
public class ModeleFacture implements ModeleDocument {

    public static final String MENTION_TVA = "Exonéré de TVA (article 261 C du CGI)";
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private final FactureRepository factures;
    private final DonneesContratRepository contrats;
    private final CalculFacture calcul;

    @Override
    public TypeDocument type() {
        return TypeDocument.FACTURE;
    }

    @Override
    public ContenuDocument rediger(Document document) {
        Facture facture = (document.getReference() == null ? factures.findFirstByDocumentId(document.getId())
                : factures.findByNumero(document.getReference()))
                .orElseThrow(() -> new IllegalStateException("Facture introuvable pour le document " + document.getId()));
        DonneesContrat contrat = contrats.lire(facture.getContratId())
                .orElseThrow(() -> new IllegalStateException("Contrat introuvable pour le document " + document.getId()));
        RepartitionTaxe.Detail detail = calcul.detail(contrat, facture.getMontantTtc());
        LocalDate date = LocalDate.ofInstant(facture.getGenereeLe(), FRANCE);

        List<Section> sections = new ArrayList<>();
        sections.add(new Section("Facture",
                "Facture n° " + facture.getNumero(),
                "Date de la facture : " + MiseEnForme.date(date)));
        sections.add(new Section("Client", MiseEnForme.personne(contrat.souscripteur().civilite(),
                contrat.souscripteur().prenom(), contrat.souscripteur().nom())));
        sections.add(new Section("Contrat",
                "Contrat n° " + contrat.numero(),
                "Assurance " + ("AUTO".equals(contrat.produit()) ? "auto" : "habitation") + ", formule "
                        + MiseEnForme.formule(contrat.formule())));
        sections.add(new Section("Période couverte",
                "Du " + MiseEnForme.date(facture.getPeriodeDebut()) + " au " + MiseEnForme.date(facture.getPeriodeFin())));
        sections.add(new Section("Détail par garantie", detail.lignes().stream().map(ModeleFacture::ligne).toList()));
        sections.add(new Section("Montant",
                "Prime hors taxe : " + MiseEnForme.montant(detail.montantHt()),
                "Taxe sur les conventions d'assurance (TSCA) : " + MiseEnForme.montant(detail.taxe()),
                "Total TTC : " + MiseEnForme.montant(detail.montantTtc()),
                MENTION_TVA));
        return new ContenuDocument("Facture", facture.getNumero(), sections);
    }

    private static String ligne(RepartitionTaxe.Ligne ligne) {
        return "• " + ligne.libelle() + " : prime HT " + MiseEnForme.montant(ligne.montantHt()) + " ; TSCA "
                + pourcentage(ligne.taux()) + " " + MiseEnForme.montant(ligne.taxe()) + " ; TTC "
                + MiseEnForme.montant(ligne.montantTtc());
    }

    /** 0.134 → « 13,4 % ». */
    static String pourcentage(BigDecimal taux) {
        return taux.multiply(CENT).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
                .replace('.', ',') + " %";
    }
}
