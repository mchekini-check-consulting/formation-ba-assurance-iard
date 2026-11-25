package net.checkconsulting.assuranceiard.api.service.document;

import java.util.ArrayList;
import java.util.List;
import net.checkconsulting.assuranceiard.api.model.DevisAuto;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DevisAutoRepository;
import org.springframework.stereotype.Component;

/**
 * US-29 étape 3 : fiche conseil (L521-4 Code des assurances) générée à l'enregistrement du devis.
 * La référence du document = identifiant du devis auto (converti en chaîne).
 */
@Component
public class ModeleFicheConseilAuto implements ModeleDocument {

    private final DevisAutoRepository devisAutos;

    public ModeleFicheConseilAuto(DevisAutoRepository devisAutos) {
        this.devisAutos = devisAutos;
    }

    @Override
    public TypeDocument type() {
        return TypeDocument.FICHE_CONSEIL;
    }

    @Override
    public boolean convientA(Document document) {
        if (document.getReference() == null) return false;
        try {
            Long.parseLong(document.getReference());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public ContenuDocument rediger(Document document) {
        Long devisAutoId = Long.parseLong(document.getReference());
        DevisAuto d = devisAutos.findById(devisAutoId)
                .orElseThrow(() -> new IllegalStateException("DevisAuto introuvable : id=" + devisAutoId));

        List<ContenuDocument.Section> sections = new ArrayList<>();
        sections.add(new ContenuDocument.Section("Devis",
                "Référence : " + d.getId(),
                "Date : " + MiseEnForme.date(java.time.LocalDate.now()),
                "Validité : " + (d.getDateFinValidite() != null ? MiseEnForme.date(d.getDateFinValidite()) : "30 jours")));

        sections.add(new ContenuDocument.Section("Formule conseillée",
                "Formule : " + MiseEnForme.formule(d.getFormuleConseillee()),
                "Raisons : " + (d.getRaisonsFormule() != null ? d.getRaisonsFormule().replace("|", " ; ") : "–")));

        sections.add(new ContenuDocument.Section("Formule choisie",
                "Formule : " + MiseEnForme.formule(d.getFormuleChoisie()),
                choixDifferentTexte(d)));

        sections.add(new ContenuDocument.Section("Options choisies",
                optionsTexte(d)));

        sections.add(new ContenuDocument.Section("Tarif",
                d.getPrixMensuelTtc() != null ? "Mensuel TTC : " + MiseEnForme.montant(d.getPrixMensuelTtc()) : "Prix non calculé",
                d.getPrixAnnuelTtc() != null ? "Annuel TTC : " + MiseEnForme.montant(d.getPrixAnnuelTtc()) : ""));

        sections.add(new ContenuDocument.Section("Vos besoins (étape 2)",
                besoins(d).toArray(new String[0])));

        return new ContenuDocument("Fiche conseil – Assurance automobile", "FC-AUTO-" + d.getId(), sections, false);
    }

    private String choixDifferentTexte(DevisAuto d) {
        if (!Boolean.TRUE.equals(d.getChoixDifferentDuConseil())) {
            return "Conforme au conseil.";
        }
        String date = d.getDateConfirmationChoixDifferent() != null
                ? MiseEnForme.date(d.getDateConfirmationChoixDifferent().atZone(java.time.ZoneId.of("Europe/Paris")).toLocalDate())
                : "–";
        return "Choix différent du conseil, confirmé par le client le " + date + ".";
    }

    private List<String> besoins(DevisAuto d) {
        List<String> lignes = new ArrayList<>();
        if (d.getValeurVehicule() != null) lignes.add("Valeur véhicule : " + d.getValeurVehicule() + " €");
        lignes.add("Véhicule indispensable : " + oui(d.getVehiculeIndispensable()));
        lignes.add("Assistance 0 km : " + oui(d.getAssistance0km()));
        lignes.add("Protection conducteur : " + oui(d.getProtectionConducteur()));
        lignes.add("Bris de glace : " + oui(d.getBrisGlace()));
        lignes.add("Accompagnement litige : " + oui(d.getAccompagnementLitige()));
        if (d.getPriorite() != null) lignes.add("Priorité : " + d.getPriorite());
        return lignes;
    }

    private String optionsTexte(DevisAuto d) {
        if (d.getOptionsChoisies() == null || d.getOptionsChoisies().isBlank()) return "Aucune option.";
        String[] codes = d.getOptionsChoisies().split(",");
        List<String> libelles = new ArrayList<>();
        for (String code : codes) libelles.add(MiseEnForme.option(code.trim()));
        return String.join(", ", libelles);
    }

    private String oui(Boolean v) {
        return Boolean.TRUE.equals(v) ? "Oui" : "Non";
    }
}
