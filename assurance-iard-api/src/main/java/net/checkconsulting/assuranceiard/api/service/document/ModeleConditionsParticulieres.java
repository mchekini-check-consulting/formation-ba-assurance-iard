package net.checkconsulting.assuranceiard.api.service.document;

import java.util.ArrayList;
import java.util.List;
import net.checkconsulting.assuranceiard.api.config.DocumentsProperties;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.service.document.ContenuDocument.Section;

/**
 * T-03 (US-21, US-33) : conditions particulières, parties communes à l'habitation et à l'auto (articles L112-2 et
 * L112-4 du Code des assurances). Chaque produit ajoute ses sections (logement, ou véhicule et conducteur).
 */
public abstract class ModeleConditionsParticulieres implements ModeleDocument {

    private final DonneesContratRepository contrats;
    private final DocumentRepository documents;
    private final DocumentsProperties reglages;

    protected ModeleConditionsParticulieres(DonneesContratRepository contrats, DocumentRepository documents,
            DocumentsProperties reglages) {
        this.contrats = contrats;
        this.documents = documents;
        this.reglages = reglages;
    }

    /** HABITATION ou AUTO. */
    protected abstract String produit();

    /** « habitation » ou « auto », dans les titres. */
    protected abstract String nomDuProduit();

    /** Sections propres au produit, après le souscripteur. */
    protected abstract List<Section> sectionsDuProduit(DonneesContrat contrat);

    @Override
    public TypeDocument type() {
        return TypeDocument.CONDITIONS_PARTICULIERES;
    }

    @Override
    public boolean convientA(Document document) {
        return document.getContratId() != null
                && contrats.produit(document.getContratId()).map(produit()::equals).orElse(false);
    }

    @Override
    public ContenuDocument rediger(Document document) {
        DonneesContrat contrat = contrats.lire(document.getContratId())
                .orElseThrow(() -> new IllegalStateException("Contrat introuvable pour le document " + document.getId()));
        List<Section> sections = new ArrayList<>();
        sections.add(sectionContrat(contrat, document.getVersion()));
        sections.add(new Section("Souscripteur", MiseEnForme.personne(contrat.souscripteur().civilite(),
                contrat.souscripteur().prenom(), contrat.souscripteur().nom())));
        sections.addAll(sectionsDuProduit(contrat));
        sections.add(sectionFormule(contrat));
        sections.add(sectionOptions(contrat));
        sections.add(new Section("Franchise", MiseEnForme.montant(contrat.tarif().franchise()) + " par sinistre"));
        sections.add(new Section("Cotisation TTC",
                MiseEnForme.montant(contrat.tarif().cotisationMensuelle()) + " par mois",
                MiseEnForme.montant(contrat.tarif().cotisationAnnuelle()) + " par an"));
        sections.add(sectionPaiement(contrat.paiement()));
        sections.add(sectionConditionsGenerales());
        return new ContenuDocument("Conditions particulières - Assurance " + nomDuProduit(), contrat.numero(),
                sections);
    }

    private static Section sectionContrat(DonneesContrat contrat, int version) {
        List<String> lignes = new ArrayList<>();
        lignes.add("Contrat n° " + contrat.numero() + " - version " + version);
        lignes.add("Date d'effet : " + MiseEnForme.date(contrat.tarif().dateEffet()));
        if (contrat.tarif().dateEffet() != null) {
            lignes.add("Échéance principale : le " + MiseEnForme.jourEtMois(contrat.tarif().dateEffet())
                    + " de chaque année (reconduction tacite)");
        }
        if (contrat.tarif().dateFin() != null) {
            lignes.add("Fin du contrat : " + MiseEnForme.date(contrat.tarif().dateFin()));
        }
        return new Section("Votre contrat", lignes);
    }

    private Section sectionFormule(DonneesContrat contrat) {
        List<String> lignes = new ArrayList<>();
        lignes.add("Formule " + MiseEnForme.formule(contrat.formule()) + ", garanties incluses :");
        List<String> garanties = reglages.garantiesDe(contrat.formule());
        if (garanties.isEmpty()) {
            lignes.add("• voir les conditions générales");
        }
        garanties.forEach(garantie -> lignes.add("• " + garantie));
        return new Section("Formule et garanties", lignes);
    }

    private static Section sectionOptions(DonneesContrat contrat) {
        if (contrat.options().isEmpty()) {
            return new Section("Options", "Aucune option souscrite");
        }
        return new Section("Options", contrat.options().stream()
                .map(option -> "• " + MiseEnForme.option(option.code()) + (option.prixMensuelTtc() == null ? ""
                        : " : " + MiseEnForme.montant(option.prixMensuelTtc()) + " par mois"))
                .toList());
    }

    private static Section sectionPaiement(DonneesContrat.Paiement paiement) {
        List<String> lignes = new ArrayList<>();
        if ("ANNUEL".equals(paiement.frequence())) {
            lignes.add("Prélèvement annuel");
        } else if (paiement.jourPrelevement() != null) {
            lignes.add("Prélèvement mensuel, le " + paiement.jourPrelevement() + " de chaque mois");
        } else {
            lignes.add("Prélèvement mensuel");
        }
        if (paiement.ibanMasque() == null) {
            lignes.add("Mandat de prélèvement SEPA pas encore signé");
        } else {
            lignes.add("Compte débité : IBAN " + paiement.ibanMasque());
            lignes.add("Référence unique du mandat (RUM) : " + paiement.rum());
        }
        return new Section("Paiement", lignes);
    }

    private Section sectionConditionsGenerales() {
        String version = documents.enVigueur(0, TypeDocument.CONDITIONS_GENERALES.name(), produit())
                .map(cg -> "version " + cg.getVersion())
                .orElse("dernière version");
        return new Section("Conditions générales et exclusions",
                "Votre contrat se compose des présentes conditions particulières et des conditions générales "
                        + "Assur'Easy " + nomDuProduit() + " (réf. CG-" + produit() + ", " + version
                        + "), disponibles dans votre espace client. En cas de contradiction, les conditions "
                        + "particulières prévalent.",
                reglages.exclusions(),
                "Les clauses de nullité, de déchéance et d'exclusion sont rédigées en caractères très apparents "
                        + "dans les conditions générales (articles L112-2 et L112-4 du Code des assurances).");
    }
}
