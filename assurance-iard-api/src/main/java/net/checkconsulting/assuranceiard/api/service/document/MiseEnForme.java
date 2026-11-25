package net.checkconsulting.assuranceiard.api.service.document;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/** T-03 : montants (« 14,90 € »), dates (« 23/11/2026 ») et libellés communs aux documents. */
public final class MiseEnForme {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter JOUR_MOIS = DateTimeFormatter.ofPattern("dd/MM");

    private static final Map<String, String> FORMULES = Map.of(
            "essentielle", "Essentielle", "confort", "Confort", "serenite", "Sérénité",
            "TIERS", "Tiers", "INTER", "Intermédiaire", "TR", "Tous risques");
    private static final Map<String, String> OPTIONS = Map.ofEntries(
            Map.entry("objets-valeur", "Objets de valeur"),
            Map.entry("assistance-depannage", "Assistance dépannage"),
            Map.entry("protection-juridique", "Protection juridique"),
            Map.entry("RC", "Responsabilité civile"),
            Map.entry("BRIS_GLACE", "Bris de glace"),
            Map.entry("ASSISTANCE_0KM", "Assistance dès 0 km"),
            Map.entry("CONDUCTEUR", "Protection du conducteur"),
            // US-29 étape 3 : codes options moteur auto
            Map.entry("VEHICULE_REMPLACEMENT", "Véhicule de remplacement"),
            Map.entry("PROTECTION_CONDUCTEUR", "Protection du conducteur renforcée"),
            Map.entry("PROTECTION_JURIDIQUE", "Protection juridique"));

    private MiseEnForme() {
    }

    public static String montant(BigDecimal valeur) {
        return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.FRANCE)).format(valeur)
                .replace('\u202F', ' ').replace('\u00A0', ' ') + " €";
    }

    public static String date(LocalDate valeur) {
        return valeur == null ? "non renseignée" : valeur.format(DATE);
    }

    public static String jourEtMois(LocalDate valeur) {
        return valeur.format(JOUR_MOIS);
    }

    /** Libellé d'une formule ; le code lui-même s'il est inconnu. */
    public static String formule(String code) {
        return code == null ? "non renseignée" : FORMULES.getOrDefault(code, code);
    }

    /** Libellé d'une option ; le code lui-même s'il est inconnu. */
    public static String option(String code) {
        return OPTIONS.getOrDefault(code, code);
    }

    /** « Madame Camille Durand » ; civilité omise si absente. */
    public static String personne(String civilite, String prenom, String nom) {
        StringBuilder texte = new StringBuilder();
        if ("madame".equalsIgnoreCase(civilite)) {
            texte.append("Madame ");
        } else if ("monsieur".equalsIgnoreCase(civilite)) {
            texte.append("Monsieur ");
        }
        if (prenom != null) {
            texte.append(prenom).append(' ');
        }
        if (nom != null) {
            texte.append(nom);
        }
        String resultat = texte.toString().trim();
        return resultat.isEmpty() ? "non renseigné" : resultat;
    }
}
