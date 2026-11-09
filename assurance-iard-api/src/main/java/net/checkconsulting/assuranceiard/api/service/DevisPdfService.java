package net.checkconsulting.assuranceiard.api.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.checkconsulting.assuranceiard.api.dto.DonneesDevisPdf;
import net.checkconsulting.assuranceiard.api.dto.LogementDevis;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

/** AC4 et RG3 de l'US-12 : un vrai PDF d'une page, aux couleurs Assur'Easy. */
@Service
public class DevisPdfService {

    private static final Color ROUGE = new Color(0xA8, 0x2B, 0x27);
    private static final Color TEXTE = new Color(0x22, 0x22, 0x22);
    private static final float MARGE = 50;
    private static final float HAUTEUR_BANDEAU = 80;
    private static final float TAILLE_TITRE = 18;
    private static final float TAILLE_SECTION = 13;
    private static final float TAILLE_TEXTE = 11;
    private static final float INTERLIGNE = 16;
    private static final float ESPACE_SECTION = 12;
    private static final float DIVISEUR_CORPS = 1000;
    private static final char TRAIT_CONDITIONNEL = '­';

    private static final Map<String, String> FORMULES =
            Map.of("essentielle", "Essentielle", "confort", "Confort", "serenite", "Sérénité");
    private static final Map<String, String> OPTIONS = Map.of(
            "objets-valeur", "Objets de valeur",
            "assistance-depannage", "Assistance dépannage",
            "protection-juridique", "Protection juridique");

    private final PDType1Font normale = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font grasse = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] generer(DonneesDevisPdf donnees) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contenu = new PDPageContentStream(document, page)) {
                bandeau(contenu, page);
                corps(contenu, page, donnees);
            }
            document.save(sortie);
            return sortie.toByteArray();
        } catch (IOException erreur) {
            throw new UncheckedIOException(erreur);
        }
    }

    private void bandeau(PDPageContentStream contenu, PDPage page) throws IOException {
        float hauteur = page.getMediaBox().getHeight();
        contenu.setNonStrokingColor(ROUGE);
        contenu.addRect(0, hauteur - HAUTEUR_BANDEAU, page.getMediaBox().getWidth(), HAUTEUR_BANDEAU);
        contenu.fill();
        contenu.setNonStrokingColor(Color.WHITE);
        ligne(contenu, grasse, TAILLE_TITRE, hauteur - HAUTEUR_BANDEAU / 2 - TAILLE_TITRE / 3, "Assur'Easy");
    }

    private void corps(PDPageContentStream contenu, PDPage page, DonneesDevisPdf donnees) throws IOException {
        float largeur = page.getMediaBox().getWidth() - 2 * MARGE;
        float y = page.getMediaBox().getHeight() - HAUTEUR_BANDEAU - MARGE;

        contenu.setNonStrokingColor(ROUGE);
        ligne(contenu, grasse, TAILLE_TITRE, y, "Mon devis d'assurance habitation");
        y -= INTERLIGNE + ESPACE_SECTION;

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
        contenu.setNonStrokingColor(ROUGE);
        ligne(contenu, grasse, TAILLE_SECTION, y, titre);
        y -= INTERLIGNE + ESPACE_SECTION / 2;
        contenu.setNonStrokingColor(TEXTE);
        for (String texte : lignes) {
            for (String morceau : decouper(lisible(texte), largeur)) {
                ligne(contenu, normale, TAILLE_TEXTE, y, morceau);
                y -= INTERLIGNE;
            }
        }
        return y - ESPACE_SECTION;
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

    private void ligne(PDPageContentStream contenu, PDType1Font police, float taille, float y, String texte)
            throws IOException {
        contenu.beginText();
        contenu.setFont(police, taille);
        contenu.newLineAtOffset(MARGE, y);
        contenu.showText(texte);
        contenu.endText();
    }

    /** Une adresse saisie peut contenir un caractère absent de la police : il ne doit pas faire échouer le PDF. */
    private String lisible(String texte) {
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

    private List<String> decouper(String texte, float largeur) throws IOException {
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

    private float largeurDe(String texte) throws IOException {
        return normale.getStringWidth(texte) / DIVISEUR_CORPS * TAILLE_TEXTE;
    }
}
