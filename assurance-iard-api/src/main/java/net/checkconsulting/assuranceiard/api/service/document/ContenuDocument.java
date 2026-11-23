package net.checkconsulting.assuranceiard.api.service.document;

import java.util.ArrayList;
import java.util.List;

/**
 * T-03 : ce qu'un modèle rédige ; la mise en page (charte, pied de page, mentions de l'assureur) est commune.
 *
 * @param titre titre de la première page.
 * @param numero numéro affiché dans le pied de page (n° de contrat, de facture...).
 * @param sections sections dans l'ordre.
 * @param provisoire vrai : chaque page porte la mention « DOCUMENT PROVISOIRE – À REMPLACER PAR LE PO ».
 */
public record ContenuDocument(String titre, String numero, List<Section> sections, boolean provisoire) {

    public ContenuDocument {
        sections = List.copyOf(sections);
    }

    public ContenuDocument(String titre, String numero, List<Section> sections) {
        this(titre, numero, sections, false);
    }

    /** Le même contenu, avec une section ajoutée à la fin. */
    public ContenuDocument avec(Section section) {
        List<Section> toutes = new ArrayList<>(sections);
        toutes.add(section);
        return new ContenuDocument(titre, numero, toutes, provisoire);
    }

    /** Un titre de section et ses lignes (une ligne trop longue passe à la ligne toute seule). */
    public record Section(String titre, List<String> lignes) {

        public Section {
            lignes = List.copyOf(lignes);
        }

        public Section(String titre, String... lignes) {
            this(titre, List.of(lignes));
        }
    }
}
