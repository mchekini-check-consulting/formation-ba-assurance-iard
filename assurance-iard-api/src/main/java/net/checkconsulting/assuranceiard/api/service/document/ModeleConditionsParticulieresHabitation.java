package net.checkconsulting.assuranceiard.api.service.document;

import java.util.List;
import net.checkconsulting.assuranceiard.api.config.DocumentsProperties;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.service.document.ContenuDocument.Section;
import org.springframework.stereotype.Component;

/** T-03 (US-21) : conditions particulières d'un contrat habitation. */
@Component
public class ModeleConditionsParticulieresHabitation extends ModeleConditionsParticulieres {

    public ModeleConditionsParticulieresHabitation(DonneesContratRepository contrats, DocumentRepository documents,
            DocumentsProperties reglages) {
        super(contrats, documents, reglages);
    }

    @Override
    protected String produit() {
        return DonneesContrat.HABITATION;
    }

    @Override
    protected String nomDuProduit() {
        return "habitation";
    }

    @Override
    protected List<Section> sectionsDuProduit(DonneesContrat contrat) {
        DonneesContrat.Logement logement = contrat.logement();
        String type = "maison".equalsIgnoreCase(logement.type()) ? "Maison" : "Appartement";
        return List.of(new Section("Logement assuré",
                type + " de " + logement.surface() + " m²",
                logement.adresse() + ", " + logement.codePostal() + " " + logement.ville()));
    }
}
