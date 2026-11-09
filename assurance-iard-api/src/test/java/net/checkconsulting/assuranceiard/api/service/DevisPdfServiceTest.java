package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.checkconsulting.assuranceiard.api.dto.DonneesDevisPdf;
import net.checkconsulting.assuranceiard.api.dto.LogementDevis;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class DevisPdfServiceTest {

    private final DevisPdfService service = new DevisPdfService();

    private static LogementDevis logement(String adresse) {
        return new LogementDevis("locataire", "principale", "appartement", adresse, "75001", "Paris", "60", "3",
                null, null, null, null, null, null, null, null);
    }

    private static DonneesDevisPdf donnees(String adresse, List<String> options) {
        return new DonneesDevisPdf(logement(adresse), "serenite", options, "300", new BigDecimal("18.50"),
                new BigDecimal("222.00"), LocalDate.of(2026, 12, 5));
    }

    private String texte(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            return new PDFTextStripper().getText(document);
        }
    }

    @Test
    void ac4LePdfContientToutCeQuiEstDemande() throws IOException {
        String contenu = texte(service.generer(donnees("12 rue des Lilas", List.of("objets-valeur"))));

        assertThat(contenu)
                .contains("Assur'Easy")
                .contains("Appartement de 60 m²")
                .contains("12 rue des Lilas, 75001 Paris")
                .contains("Sérénité")
                .contains("franchise 300 €")
                .contains("Objets de valeur")
                .contains("18,50 € par mois")
                .contains("222,00 € par an")
                .contains("05/12/2026");
    }

    @Test
    void indiqueLAbsenceDOption() throws IOException {
        assertThat(texte(service.generer(donnees("12 rue des Lilas", List.of())))).contains("Aucune option");
    }

    @Test
    void uneAdresseAvecDesCaracteresInconnusDeLaPoliceNeBloquePasLePdf() throws IOException {
        String tousLesCaracteres = IntStream.rangeClosed(0, 0xFFFF)
                .filter(code -> !Character.isSurrogate((char) code))
                .limit(255)
                .mapToObj(code -> String.valueOf((char) code))
                .collect(Collectors.joining());

        assertThat(texte(service.generer(donnees(tousLesCaracteres + " 日本 😀 Œuvre", List.of())))).contains("Tarif TTC");
        assertThat(texte(service.generer(donnees("rue 日本 😀", List.of())))).contains("rue ?? ?");
    }

    @Test
    void uneTresLongueAdresseResteDansLaPage() throws IOException {
        String contenu = texte(service.generer(donnees("Résidence ".repeat(25) + "x".repeat(200), List.of())));

        assertThat(contenu).contains("Validité").contains("xxxxx");
    }
}
