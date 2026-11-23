package net.checkconsulting.assuranceiard.api.service.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.checkconsulting.assuranceiard.api.config.DocumentsProperties;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.service.document.ContenuDocument.Section;
import org.springframework.stereotype.Component;

/**
 * T-03 : conditions générales PROVISOIRES, une par produit (référence HABITATION ou AUTO), en attendant les PDF
 * fournis par le PO. Chaque page porte la mention « DOCUMENT PROVISOIRE – À REMPLACER PAR LE PO ».
 */
@Component
public class ModeleConditionsGenerales implements ModeleDocument {

    /** Produits qui ont des conditions générales, avec leurs formules dans l'ordre. */
    public static final Map<String, List<String>> FORMULES_PAR_PRODUIT = Map.of(
            DonneesContrat.HABITATION, List.of("essentielle", "confort", "serenite"),
            DonneesContrat.AUTO, List.of("TIERS", "INTER", "TR"));

    private final DocumentsProperties reglages;

    public ModeleConditionsGenerales(DocumentsProperties reglages) {
        this.reglages = reglages;
    }

    @Override
    public TypeDocument type() {
        return TypeDocument.CONDITIONS_GENERALES;
    }

    @Override
    public boolean convientA(Document document) {
        return document.getReference() != null && FORMULES_PAR_PRODUIT.containsKey(document.getReference());
    }

    @Override
    public ContenuDocument rediger(Document document) {
        String produit = document.getReference();
        String nom = DonneesContrat.AUTO.equals(produit) ? "auto" : "habitation";
        List<Section> sections = new ArrayList<>();
        sections.add(new Section("Document provisoire",
                "Ces conditions générales sont un modèle provisoire, généré pour la recette. Elles seront "
                        + "remplacées par le document fourni par le PO, sans valeur contractuelle d'ici là."));
        sections.add(new Section("Objet du contrat",
                "Le contrat Assur'Easy " + nom + " garantit l'assuré contre les risques décrits ci-dessous, dans "
                        + "les limites de la formule et des options choisies aux conditions particulières."));
        List<String> garanties = new ArrayList<>();
        for (String formule : FORMULES_PAR_PRODUIT.get(produit)) {
            garanties.add("Formule " + MiseEnForme.formule(formule) + " : "
                    + String.join(", ", reglages.garantiesDe(formule)));
        }
        sections.add(new Section("Garanties selon la formule", garanties));
        sections.add(new Section("Exclusions communes",
                "Sont exclus les dommages causés intentionnellement par l'assuré, ceux résultant de la guerre et "
                        + "ceux exclus par la loi (texte provisoire, à remplacer par le PO).",
                reglages.exclusions()));
        sections.add(new Section("Vie du contrat",
                "Le contrat est conclu pour un an et se renouvelle tacitement à chaque échéance principale. Toute "
                        + "modification fait l'objet d'un avenant."));
        sections.add(new Section("Résiliation",
                "Le contrat peut être résilié à l'échéance principale, puis à tout moment après un an "
                        + "d'engagement (loi Hamon), dans les conditions prévues par le Code des assurances."));
        return new ContenuDocument("Conditions générales - Assurance " + nom, "CG-" + produit, sections, true);
    }
}
