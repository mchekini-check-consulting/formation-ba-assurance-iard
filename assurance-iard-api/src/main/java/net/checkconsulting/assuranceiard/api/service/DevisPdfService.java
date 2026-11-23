package net.checkconsulting.assuranceiard.api.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.checkconsulting.assuranceiard.api.dto.DonneesDevisPdf;
import net.checkconsulting.assuranceiard.api.dto.LogementDevis;
import net.checkconsulting.assuranceiard.api.service.document.ChartePdf;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.stereotype.Service;

/** AC4 et RG3 de l'US-12 : un vrai PDF d'une page, aux couleurs Assur'Easy (charte partagée, {@link ChartePdf}). */
@Service
public class DevisPdfService {

    private static final Map<String, String> FORMULES =
            Map.of("essentielle", "Essentielle", "confort", "Confort", "serenite", "Sérénité");
    private static final Map<String, String> OPTIONS = Map.of(
            "objets-valeur", "Objets de valeur",
            "assistance-depannage", "Assistance dépannage",
            "protection-juridique", "Protection juridique");

    private final ChartePdf charte = new ChartePdf();

    public byte[] generer(DonneesDevisPdf donnees) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contenu = new PDPageContentStream(document, page)) {
                charte.bandeau(contenu, page);
                corps(contenu, page, donnees);
            }
            document.save(sortie);
            return sortie.toByteArray();
        } catch (IOException erreur) {
            throw new UncheckedIOException(erreur);
        }
    }

    private void corps(PDPageContentStream contenu, PDPage page, DonneesDevisPdf donnees) throws IOException {
        float largeur = page.getMediaBox().getWidth() - 2 * ChartePdf.MARGE;
        float y = page.getMediaBox().getHeight() - ChartePdf.HAUTEUR_BANDEAU - ChartePdf.MARGE;

        contenu.setNonStrokingColor(ChartePdf.ROUGE);
        charte.ligne(contenu, charte.grasse(), ChartePdf.TAILLE_TITRE, y, "Mon devis d'assurance habitation");
        y -= ChartePdf.INTERLIGNE + ChartePdf.ESPACE_SECTION;

        y = section(contenu, y, largeur, "Logement", lignesLogement(donnees.logement()));
        y = section(contenu, y, largeur, "Formule", List.of(FORMULES.get(donnees.formule())
                + " - franchise " + donnees.franchise() + " €"));
        y = section(contenu, y, largeur, "Options", lignesOptions(donnees.options()));
        y = section(contenu, y, largeur, "Tarif TTC", List.of(
                prix(donnees.prixMensuel()) + " € par mois",
                prix(donnees.prixAnnuel()) + " € par an"));
        section(contenu, y, largeur, "Validité", List.of(
                "Devis valable jusqu'au "
                        + donnees.dateFinValidite().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + " : prix garanti jusqu'à cette date.",
                "Ce document n'est pas un contrat d'assurance."));
    }

    private float section(PDPageContentStream contenu, float depart, float largeur, String titre, List<String> lignes)
            throws IOException {
        float y = depart;
        contenu.setNonStrokingColor(ChartePdf.ROUGE);
        charte.ligne(contenu, charte.grasse(), ChartePdf.TAILLE_SECTION, y, titre);
        y -= ChartePdf.INTERLIGNE + ChartePdf.ESPACE_SECTION / 2;
        contenu.setNonStrokingColor(ChartePdf.TEXTE);
        for (String texte : lignes) {
            for (String morceau : charte.decouper(charte.lisible(texte), largeur)) {
                charte.ligne(contenu, charte.normale(), ChartePdf.TAILLE_TEXTE, y, morceau);
                y -= ChartePdf.INTERLIGNE;
            }
        }
        return y - ChartePdf.ESPACE_SECTION;
    }

    private List<String> lignesLogement(LogementDevis logement) {
        String type = "maison".equals(logement.typeLogement()) ? "Maison" : "Appartement";
        return List.of(
                type + " de " + logement.surface() + " m²",
                logement.adresse() + ", " + logement.codePostal() + " " + logement.ville());
    }

    private List<String> lignesOptions(List<String> options) {
        return options.isEmpty() ? List.of("Aucune option") : options.stream().map(OPTIONS::get).toList();
    }

    private String prix(BigDecimal montant) {
        return new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.FRANCE)).format(montant);
    }
}
