package net.checkconsulting.assuranceiard.api.service.document;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.checkconsulting.assuranceiard.api.config.DocumentsProperties;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.service.document.ContenuDocument.Section;
import org.springframework.stereotype.Component;

/** T-03 (US-21, US-33) : conditions particulières d'un contrat auto (véhicule, conducteur, ancien contrat). */
@Component
public class ModeleConditionsParticulieresAuto extends ModeleConditionsParticulieres {

    public ModeleConditionsParticulieresAuto(DonneesContratRepository contrats, DocumentRepository documents,
            DocumentsProperties reglages) {
        super(contrats, documents, reglages);
    }

    @Override
    protected String produit() {
        return DonneesContrat.AUTO;
    }

    @Override
    protected String nomDuProduit() {
        return "auto";
    }

    @Override
    protected List<Section> sectionsDuProduit(DonneesContrat contrat) {
        List<Section> sections = new ArrayList<>();
        DonneesContrat.Vehicule vehicule = contrat.vehicule();
        String modele = String.join(" ", vehicule.marque(), vehicule.modele(),
                vehicule.version() == null ? "" : vehicule.version()).trim();
        sections.add(new Section("Véhicule assuré", modele, vehicule.immatriculation() == null
                ? "Véhicule pas encore immatriculé" : "Immatriculation : " + vehicule.immatriculation()));

        DonneesContrat.Conducteur conducteur = contrat.conducteur();
        sections.add(new Section("Conducteur principal",
                Boolean.FALSE.equals(conducteur.estSouscripteur()) ? "Conducteur désigné au contrat"
                        : "Le souscripteur",
                "Coefficient de réduction-majoration (bonus-malus) : " + (conducteur.coefficientBonusMalus() == null
                        ? "non renseigné"
                        : new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.FRANCE))
                                .format(conducteur.coefficientBonusMalus()))));

        if (contrat.ancienContrat() != null) {
            sections.add(new Section("Ancien contrat résilié",
                    "Assur'Easy résilie pour vous le contrat n° " + contrat.ancienContrat().numero() + " souscrit auprès de "
                            + contrat.ancienContrat().assureur() + " (loi Hamon)."));
        }
        return sections;
    }
}
