package net.checkconsulting.assuranceiard.api.service.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import net.checkconsulting.assuranceiard.api.service.document.ContenuDocument.Section;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class GabaritPdfTest {

    private static final GabaritPdf.Pied PIED =
            new GabaritPdf.Pied("Conditions particulières", "AE-2026-000001", 2, LocalDate.of(2026, 11, 23));

    private final GabaritPdf gabarit = new GabaritPdf();

    static String page(byte[] pdf, int numero) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper extracteur = new PDFTextStripper();
            extracteur.setStartPage(numero);
            extracteur.setEndPage(numero);
            return extracteur.getText(document);
        }
    }

    static int pages(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
    }

    @Test
    void unDocumentCourtTientSurUnePageAvecSonPiedDePage() throws IOException {
        byte[] pdf = gabarit.rendre(new ContenuDocument("Mon titre", "AE-2026-000001",
                List.of(new Section("Franchise", "300,00 €"))), PIED);

        assertThat(pages(pdf)).isEqualTo(1);
        assertThat(page(pdf, 1)).contains("Assur'Easy", "Mon titre", "Franchise", "300,00 €")
                .contains("Conditions particulières · n° AE-2026-000001 · version 2 · généré le 23/11/2026 · page 1/1")
                .doesNotContain(GabaritPdf.MENTION_PROVISOIRE);
    }

    @Test
    void unLongDocumentContinueSurDesPagesNumerotees() throws IOException {
        List<String> lignes = Collections.nCopies(120, "Une ligne de garanties assez longue pour remplir la page.");
        byte[] pdf = gabarit.rendre(new ContenuDocument("Titre", "AE-2026-000001",
                List.of(new Section("Garanties", lignes), new Section("Fin", "Dernière ligne"))), PIED);

        int total = pages(pdf);
        assertThat(total).isGreaterThan(2);
        for (int numero = 1; numero <= total; numero++) {
            assertThat(page(pdf, numero)).contains("Assur'Easy").contains("page " + numero + "/" + total);
        }
        assertThat(page(pdf, total)).contains("Dernière ligne");
    }

    @Test
    void unDocumentProvisoirePorteLaMentionSurChaquePage() throws IOException {
        List<String> lignes = Collections.nCopies(80, "Texte provisoire.");
        byte[] pdf = gabarit.rendre(new ContenuDocument("Conditions générales", "CG-AUTO",
                List.of(new Section("Objet", lignes)), true), PIED);

        int total = pages(pdf);
        assertThat(total).isGreaterThan(1);
        for (int numero = 1; numero <= total; numero++) {
            assertThat(page(pdf, numero)).contains(GabaritPdf.MENTION_PROVISOIRE);
        }
    }

    @Test
    void desCaracteresInconnusDeLaPoliceNeBloquentPasLePdf() throws IOException {
        byte[] pdf = gabarit.rendre(new ContenuDocument("Titre 日本", "AE-1",
                List.of(new Section("Adresse", "rue 😀 de l'Œuvre", "IBAN •••• 4821"))), PIED);

        assertThat(page(pdf, 1)).contains("rue ? de l'Œuvre").contains("IBAN •••• 4821");
    }
}
