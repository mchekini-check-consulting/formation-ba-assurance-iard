package net.checkconsulting.assuranceiard.api.service.document;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.service.document.ContenuDocument.Section;
import org.springframework.stereotype.Component;

/** US-33 (§5) : mémo véhicule assuré d'un contrat auto. */
@Component
public class ModeleMemoVehiculeAssure implements ModeleDocument {

    private static final DecimalFormat FORMAT_FRANCHISE =
            new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.FRANCE));

    private final DonneesContratRepository contrats;

    public ModeleMemoVehiculeAssure(DonneesContratRepository contrats) {
        this.contrats = contrats;
    }

    @Override
    public TypeDocument type() {
        return TypeDocument.MEMO_VEHICULE;
    }

    @Override
    public boolean convientA(Document document) {
        return document.getContratId() != null
                && DonneesContrat.AUTO.equals(contrats.produit(document.getContratId()).orElse(null));
    }

    @Override
    public ContenuDocument rediger(Document document) {
        DonneesContrat contrat = contrats.lire(document.getContratId())
                .orElseThrow(() -> new IllegalStateException("Contrat introuvable : " + document.getContratId()));

        DonneesContrat.Vehicule vehicule = contrat.vehicule();
        DonneesContrat.Tarif tarif = contrat.tarif();

        List<Section> sections = new ArrayList<>();

        // Véhicule
        String modeleComplet = String.join(" ",
                vehicule.marque() != null ? vehicule.marque() : "",
                vehicule.modele() != null ? vehicule.modele() : "",
                vehicule.version() != null ? vehicule.version() : "").trim();
        String immatriculation = vehicule.immatriculation() != null
                ? "Immatriculation : " + vehicule.immatriculation()
                : "Pas encore immatriculé";
        sections.add(new Section("Véhicule assuré", modeleComplet, immatriculation));

        // Contrat
        String formuleLabel = nomFormule(contrat.formule());
        String dateEffetStr = tarif.dateEffet() != null
                ? formatDate(tarif.dateEffet()) : "—";
        sections.add(new Section("Votre assurance auto",
                "Contrat n° " + contrat.numero(),
                "Formule : " + formuleLabel,
                "Date de prise d'effet : " + dateEffetStr));

        // Franchise
        String franchiseStr = tarif.franchise() != null && tarif.franchise().signum() > 0
                ? FORMAT_FRANCHISE.format(tarif.franchise()) + " €"
                : "Aucune franchise";
        sections.add(new Section("Franchise", franchiseStr));

        // Validité
        if (tarif.dateEffet() != null) {
            String fin = formatDate(tarif.dateEffet().plusYears(1).minusDays(1));
            sections.add(new Section("Période de validité",
                    "Du " + dateEffetStr + " au " + fin));
        }

        return new ContenuDocument("Mémo véhicule assuré", contrat.numero(), sections);
    }

    private static String nomFormule(String code) {
        if (code == null) return "—";
        return switch (code) {
            case "TIERS" -> "Tiers";
            case "INTER" -> "Intermédiaire";
            case "TR" -> "Tous risques";
            default -> code;
        };
    }

    private static String formatDate(java.time.LocalDate date) {
        return String.format("%02d/%02d/%d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }
}
