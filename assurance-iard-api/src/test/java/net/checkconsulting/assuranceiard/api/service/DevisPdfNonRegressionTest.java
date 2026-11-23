package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.checkconsulting.assuranceiard.api.dto.DonneesDevisPdf;
import net.checkconsulting.assuranceiard.api.dto.LogementDevis;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

/** T-03 : la charte est partagée avec les documents générés ; le PDF du devis habitation reste identique. */
class DevisPdfNonRegressionTest {

    private static final String TEXTE_ATTENDU = String.join("\n",
            "Assur'Easy",
            "Mon devis d'assurance habitation",
            "Logement",
            "Appartement de 60 m²",
            "12 rue des Lilas, 75001 Paris",
            "Formule",
            "Sérénité - franchise 300 €",
            "Options",
            "Objets de valeur",
            "Protection juridique",
            "Tarif TTC",
            "18,50 € par mois",
            "222,00 € par an",
            "Validité",
            "Devis valable jusqu'au 05/12/2026 : prix garanti jusqu'à cette date.",
            "Ce document n'est pas un contrat d'assurance.");

    private static DonneesDevisPdf donnees() {
        LogementDevis logement = new LogementDevis("locataire", "principale", "appartement", "12 rue des Lilas",
                "75001", "Paris", "60", "3", null, null, null, null, null, null, null, null);
        return new DonneesDevisPdf(logement, "serenite", List.of("objets-valeur", "protection-juridique"), "300",
                new BigDecimal("18.50"), new BigDecimal("222.00"), LocalDate.of(2026, 12, 5));
    }

    @Test
    void leTexteEtSaMiseEnPageSontInchanges() throws IOException {
        byte[] pdf = new DevisPdfService().generer(donnees());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            PDFTextStripper extracteur = new PDFTextStripper();
            extracteur.setLineSeparator("\n");
            assertThat(extracteur.getText(document).strip()).isEqualTo(TEXTE_ATTENDU);

            Positions positions = new Positions();
            positions.getText(document);
            assertThat(String.join(" | ", positions.lignes)).isEqualTo(POSITIONS_ATTENDUES);
        }
    }

    /** Relevé sur le PDF du devis avant T-03 : « texte@x,y-taille » du début de chaque ligne. */
    private static final String POSITIONS_ATTENDUES = String.join(" | ",
            "Assur'Easy@50.0,46.0-18.0",
            "Mon devis d'assurance habitation@50.0,130.0-18.0",
            "Logement@50.0,158.0-13.0",
            "Appartement de 60 m²@50.0,180.0-11.0",
            "12 rue des Lilas, 75001 Paris@50.0,196.0-11.0",
            "Formule@50.0,224.0-13.0",
            "Sérénité - franchise 300 €@50.0,246.0-11.0",
            "Options@50.0,274.0-13.0",
            "Objets de valeur@50.0,296.0-11.0",
            "Protection juridique@50.0,312.0-11.0",
            "Tarif TTC@50.0,340.0-13.0",
            "18,50 € par mois@50.0,362.0-11.0",
            "222,00 € par an@50.0,378.0-11.0",
            "Validité@50.0,406.0-13.0",
            "Devis valable jusqu'au 05/12/2026 : prix garanti jusqu'à cette date.@50.0,428.0-11.0",
            "Ce document n'est pas un contrat d'assurance.@50.0,444.0-11.0");

    /** Position et taille du début de chaque ligne : la mise en page n'a pas bougé. */
    private static final class Positions extends PDFTextStripper {
        private final List<String> lignes = new ArrayList<>();

        private Positions() throws IOException {
            super();
        }

        @Override
        protected void writeString(String texte, List<TextPosition> lettres) {
            lignes.add(position(texte, lettres.get(0)));
        }

        private static String position(String texte, TextPosition lettre) {
            return String.format(Locale.ROOT, "%s@%.1f,%.1f-%.1f", texte, lettre.getXDirAdj(), lettre.getYDirAdj(),
                    lettre.getFontSizeInPt());
        }
    }
}
