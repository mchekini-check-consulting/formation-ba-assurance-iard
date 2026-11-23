package net.checkconsulting.assuranceiard.api.service.document;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Charte Assur'Easy des PDF (US-12, reprise par T-03) : bandeau rouge, polices, marges, coupure des lignes.
 * Partagée par le devis habitation et les documents générés.
 */
public final class ChartePdf {

    public static final Color ROUGE = new Color(0xA8, 0x2B, 0x27);
    public static final Color TEXTE = new Color(0x22, 0x22, 0x22);
    public static final float MARGE = 50;
    public static final float HAUTEUR_BANDEAU = 80;
    public static final float TAILLE_TITRE = 18;
    public static final float TAILLE_SECTION = 13;
    public static final float TAILLE_TEXTE = 11;
    public static final float INTERLIGNE = 16;
    public static final float ESPACE_SECTION = 12;

    private static final float DIVISEUR_CORPS = 1000;
    private static final char TRAIT_CONDITIONNEL = '­';

    private final PDType1Font normale = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font grasse = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public PDType1Font normale() {
        return normale;
    }

    public PDType1Font grasse() {
        return grasse;
    }

    /** Bandeau rouge « Assur'Easy » en haut de la page. */
    public void bandeau(PDPageContentStream contenu, PDPage page) throws IOException {
        float hauteur = page.getMediaBox().getHeight();
        contenu.setNonStrokingColor(ROUGE);
        contenu.addRect(0, hauteur - HAUTEUR_BANDEAU, page.getMediaBox().getWidth(), HAUTEUR_BANDEAU);
        contenu.fill();
        contenu.setNonStrokingColor(Color.WHITE);
        ligne(contenu, grasse, TAILLE_TITRE, hauteur - HAUTEUR_BANDEAU / 2 - TAILLE_TITRE / 3, "Assur'Easy");
    }

    /** Une ligne de texte à la marge gauche. */
    public void ligne(PDPageContentStream contenu, PDType1Font police, float taille, float y, String texte)
            throws IOException {
        contenu.beginText();
        contenu.setFont(police, taille);
        contenu.newLineAtOffset(MARGE, y);
        contenu.showText(texte);
        contenu.endText();
    }

    /** Un texte saisi peut contenir un caractère absent de la police : il ne doit pas faire échouer le PDF. */
    public String lisible(String texte) {
        CharsetEncoder encodeur = Charset.forName("windows-1252").newEncoder();
        StringBuilder resultat = new StringBuilder();
        texte.codePoints().forEach(point -> {
            if (Character.isISOControl(point) || Character.isSpaceChar(point) || point == TRAIT_CONDITIONNEL) {
                resultat.append(' ');
            } else if (Character.isBmpCodePoint(point) && encodeur.canEncode((char) point)) {
                resultat.append((char) point);
            } else {
                resultat.append('?');
            }
        });
        return resultat.toString();
    }

    /** Coupe un texte (police normale, taille du texte courant) en lignes qui tiennent dans la largeur. */
    public List<String> decouper(String texte, float largeur) throws IOException {
        List<String> lignes = new ArrayList<>();
        StringBuilder courante = new StringBuilder();
        for (String mot : texte.trim().split(" +")) {
            String essai = courante.isEmpty() ? mot : courante + " " + mot;
            if (largeurDe(essai) <= largeur) {
                courante = new StringBuilder(essai);
            } else {
                if (!courante.isEmpty()) {
                    lignes.add(courante.toString());
                }
                courante = couperMot(mot, largeur, lignes);
            }
        }
        lignes.add(courante.toString());
        return lignes;
    }

    /** Un mot plus long que la page est coupé. */
    private StringBuilder couperMot(String mot, float largeur, List<String> lignes) throws IOException {
        StringBuilder reste = new StringBuilder();
        for (char caractere : mot.toCharArray()) {
            if (largeurDe(reste.toString() + caractere) > largeur) {
                lignes.add(reste.toString());
                reste.setLength(0);
            }
            reste.append(caractere);
        }
        return reste;
    }

    public float largeurDe(String texte) throws IOException {
        return largeurDe(normale, TAILLE_TEXTE, texte);
    }

    public float largeurDe(PDType1Font police, float taille, String texte) throws IOException {
        return police.getStringWidth(texte) / DIVISEUR_CORPS * taille;
    }
}
