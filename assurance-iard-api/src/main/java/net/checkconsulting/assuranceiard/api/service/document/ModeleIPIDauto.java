package net.checkconsulting.assuranceiard.api.service.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import org.springframework.stereotype.Component;

/**
 * US-29 étape 3 : IPID (Document d'information produit) provisoire pour l'assurance auto.
 * Un document par formule (référence = TIERS / INTER / TR), créé au démarrage par RepriseDocuments.
 * Chaque page porte « DOCUMENT PROVISOIRE – À REMPLACER PAR LE PO ».
 */
@Component
public class ModeleIPIDauto implements ModeleDocument {

    public static final String TIERS = "TIERS";
    public static final String INTER = "INTER";
    public static final String TR = "TR";

    /** Formules auto qui ont un IPID (codes moteur tarifaire). */
    public static final Set<String> FORMULES_AUTO = Set.of(TIERS, INTER, TR);

    @Override
    public TypeDocument type() {
        return TypeDocument.IPID;
    }

    @Override
    public boolean convientA(Document document) {
        return document.getReference() != null && FORMULES_AUTO.contains(document.getReference());
    }

    @Override
    public ContenuDocument rediger(Document document) {
        String codeFormule = document.getReference();
        String nomFormule = MiseEnForme.formule(codeFormule);
        List<ContenuDocument.Section> sections = new ArrayList<>();

        sections.add(new ContenuDocument.Section("Document provisoire",
                "Cet IPID est un modèle provisoire, généré pour la recette. Il sera remplacé par le "
                        + "document fourni par le PO, sans valeur contractuelle d'ici là."));

        sections.add(new ContenuDocument.Section("Produit",
                "Assurance automobile Assur'Easy – Formule " + nomFormule,
                "Contrat à adhésion individuelle régi par le Code des assurances."));

        sections.add(new ContenuDocument.Section("Garanties principales",
                garantiesPrincipales(codeFormule).toArray(new String[0])));

        sections.add(new ContenuDocument.Section("Ce qui n'est pas couvert",
                "Conducteur sans permis valable.",
                "Dommages causés volontairement.",
                "Compétitions et transport de matières dangereuses.",
                "Usure normale ou défaut d'entretien."));

        sections.add(new ContenuDocument.Section("Franchise",
                franchiseTexte(codeFormule)));

        sections.add(new ContenuDocument.Section("Durée et résiliation",
                "Contrat conclu pour un an, renouvelable par tacite reconduction.",
                "Résiliable à l'échéance ou à tout moment après un an (loi Hamon)."));

        sections.add(new ContenuDocument.Section("Réclamations",
                "Adressez vos réclamations à votre conseiller Assur'Easy.",
                "En cas de désaccord persistant, vous pouvez saisir le médiateur."));

        return new ContenuDocument("Document d'information sur le produit d'assurance – Formule " + nomFormule,
                "IPID-AUTO-" + codeFormule, sections, true);
    }

    private List<String> garantiesPrincipales(String code) {
        List<String> g = new ArrayList<>();
        g.add("Responsabilité civile (obligatoire).");
        g.add("Défense pénale et recours.");
        if (INTER.equals(code) || TR.equals(code)) {
            g.add("Vol et incendie.");
            g.add("Bris de glace.");
            g.add("Événements climatiques.");
            g.add("Catastrophes naturelles.");
        }
        if (TR.equals(code)) {
            g.add("Dommages tous accidents (y compris si vous êtes responsable).");
        }
        return g;
    }

    private String franchiseTexte(String code) {
        return switch (code) {
            case INTER -> "Franchise : 400 €.";
            case TR -> "Franchise : 500 €.";
            default -> "Pas de franchise : la formule Tiers ne couvre pas les dommages à votre véhicule.";
        };
    }
}
