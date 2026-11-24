package net.checkconsulting.assuranceiard.api.service.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.stereotype.Component;

/**
 * T-03 : met en page un {@link ContenuDocument} avec la charte Assur'Easy, sur autant de pages A4 que nécessaire.
 * Chaque page : bandeau, mention « provisoire » s'il y a lieu, pied de page (type, numéro, version, date, page).
 */
@Component
public class GabaritPdf {

    public static final String MENTION_PROVISOIRE = "DOCUMENT PROVISOIRE – À REMPLACER PAR LE PO";

    private static final float TAILLE_PIED = 8;
    private static final float HAUT_PIED = 30;
    private static final float BAS_DU_TEXTE = ChartePdf.MARGE + HAUT_PIED;
    private static final float ESPACE_MENTION = 20;

    private final ChartePdf charte = new ChartePdf();

    /** Ce que le pied de page indique. */
    public record Pied(String type, String numero, int version, LocalDate dateGeneration) {
    }

    /**
     * US-21 : ce que le PDF retient dans ses propriétés (pas de colonne en base) : la date de sa production et
     * l'empreinte de son contenu, hors pied de page (version et date de génération).
     *
     * @param empreinteContenu vide pour un PDF produit avant US-21
     */
    public record Production(Instant produitLe, String empreinteContenu) {
    }

    static final String CLE_EMPREINTE = "AssurEasy-Empreinte-Contenu";

    public byte[] rendre(ContenuDocument contenu, Pied pied) {
        return rendre(contenu, pied, Instant.now());
    }

    /** Met en page et range dans les propriétés du PDF sa date de production et l'empreinte de son contenu. */
    public byte[] rendre(ContenuDocument contenu, Pied pied, Instant produitLe) {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            new Redaction(pdf, contenu.provisoire()).ecrire(contenu);
            piedsDePage(pdf, pied);
            PDDocumentInformation proprietes = pdf.getDocumentInformation();
            proprietes.setCreationDate(GregorianCalendar.from(produitLe.atZone(ZoneOffset.UTC)));
            proprietes.setCustomMetadataValue(CLE_EMPREINTE, empreinte(contenu));
            pdf.save(sortie);
            return sortie.toByteArray();
        } catch (IOException erreur) {
            throw new UncheckedIOException(erreur);
        }
    }

    /** US-21 : relit la date de production et l'empreinte du contenu d'un PDF ; vide si illisible ou sans date. */
    public Optional<Production> production(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDDocumentInformation proprietes = document.getDocumentInformation();
            Calendar creation = proprietes.getCreationDate();
            if (creation == null) {
                return Optional.empty();
            }
            return Optional.of(new Production(creation.toInstant(),
                    proprietes.getCustomMetadataValue(CLE_EMPREINTE)));
        } catch (IOException illisible) {
            return Optional.empty();
        }
    }

    /**
     * US-21 : empreinte SHA-256 du contenu rédigé (titre, numéro, sections). Deux contenus identiques, rédigés avec les
     * mêmes données, ont la même empreinte ; le pied de page (version, date de génération) n'en fait pas partie.
     */
    public static String empreinte(ContenuDocument contenu) {
        try {
            byte[] hache = MessageDigest.getInstance("SHA-256")
                    .digest(contenu.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hache);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private void piedsDePage(PDDocument pdf, Pied pied) throws IOException {
        int total = pdf.getNumberOfPages();
        for (int rang = 0; rang < total; rang++) {
            PDPage page = pdf.getPage(rang);
            try (PDPageContentStream flux = new PDPageContentStream(pdf, page, AppendMode.APPEND, true, true)) {
                flux.setNonStrokingColor(ChartePdf.TEXTE);
                charte.ligne(flux, charte.normale(), TAILLE_PIED, HAUT_PIED, charte.lisible(String.format(
                        "%s · n° %s · version %d · généré le %s · page %d/%d", pied.type(), pied.numero(),
                        pied.version(), MiseEnForme.date(pied.dateGeneration()), rang + 1, total)));
            }
        }
    }

    /** Écriture page après page ; une nouvelle page commence quand la place manque. */
    private final class Redaction {

        private final PDDocument pdf;
        private final boolean provisoire;
        private PDPageContentStream flux;
        private float largeur;
        private float y;

        private Redaction(PDDocument pdf, boolean provisoire) {
            this.pdf = pdf;
            this.provisoire = provisoire;
        }

        private void ecrire(ContenuDocument contenu) throws IOException {
            nouvellePage();
            try {
                flux.setNonStrokingColor(ChartePdf.ROUGE);
                for (String morceau : charte.decouper(charte.lisible(contenu.titre()), largeur * ChartePdf.TAILLE_TEXTE
                        / ChartePdf.TAILLE_TITRE)) {
                    charte.ligne(flux, charte.grasse(), ChartePdf.TAILLE_TITRE, y, morceau);
                    y -= ChartePdf.INTERLIGNE + ChartePdf.ESPACE_SECTION / 2;
                }
                y -= ChartePdf.ESPACE_SECTION / 2;
                for (ContenuDocument.Section section : contenu.sections()) {
                    section(section.titre(), section.lignes());
                }
            } finally {
                flux.close();
            }
        }

        private void section(String titre, List<String> lignes) throws IOException {
            placePour(2 * ChartePdf.INTERLIGNE);
            flux.setNonStrokingColor(ChartePdf.ROUGE);
            charte.ligne(flux, charte.grasse(), ChartePdf.TAILLE_SECTION, y, charte.lisible(titre));
            y -= ChartePdf.INTERLIGNE + ChartePdf.ESPACE_SECTION / 2;
            flux.setNonStrokingColor(ChartePdf.TEXTE);
            for (String texte : lignes) {
                for (String morceau : charte.decouper(charte.lisible(texte), largeur)) {
                    placePour(ChartePdf.INTERLIGNE);
                    charte.ligne(flux, charte.normale(), ChartePdf.TAILLE_TEXTE, y, morceau);
                    y -= ChartePdf.INTERLIGNE;
                }
            }
            y -= ChartePdf.ESPACE_SECTION;
        }

        private void placePour(float hauteur) throws IOException {
            if (y - hauteur < BAS_DU_TEXTE) {
                flux.close();
                nouvellePage();
                flux.setNonStrokingColor(ChartePdf.TEXTE);
            }
        }

        private void nouvellePage() throws IOException {
            PDPage page = new PDPage(PDRectangle.A4);
            pdf.addPage(page);
            flux = new PDPageContentStream(pdf, page);
            charte.bandeau(flux, page);
            largeur = page.getMediaBox().getWidth() - 2 * ChartePdf.MARGE;
            y = page.getMediaBox().getHeight() - ChartePdf.HAUTEUR_BANDEAU - ChartePdf.MARGE;
            if (provisoire) {
                flux.setNonStrokingColor(ChartePdf.ROUGE);
                charte.ligne(flux, charte.grasse(), ChartePdf.TAILLE_TEXTE, y + ESPACE_MENTION, MENTION_PROVISOIRE);
                y -= ChartePdf.ESPACE_SECTION;
            }
        }
    }
}
