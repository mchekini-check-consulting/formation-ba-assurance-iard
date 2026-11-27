package net.checkconsulting.assuranceiard.api.service.sinistre;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import net.checkconsulting.assuranceiard.api.dto.MiseAJourSinistreRequest;
import net.checkconsulting.assuranceiard.api.exception.DeclarationInvalideException;
import net.checkconsulting.assuranceiard.api.model.Sinistre;

/**
 * US-24 : contrôles des réponses, avec un message clair (jamais technique) pour le client.
 * <ul>
 *   <li>À chaque enregistrement : formats et longueurs, date jamais dans le futur (AC4) ; un brouillon peut rester
 *       incomplet (AC8).</li>
 *   <li>À l'envoi : toutes les réponses obligatoires (AC4, AC6, AC11, AC13).</li>
 * </ul>
 */
public final class ValidationDeclaration {

    public static final String SOUSCRIPTEUR = "SOUSCRIPTEUR";
    static final Set<String> DECLARANTS = Set.of(SOUSCRIPTEUR, "CONDUCTEUR", "PROCHE");

    static final String TYPE_INCONNU = "Choisissez ce qui s'est passé parmi les propositions.";
    static final String DATE_FUTURE = "La date du sinistre ne peut pas être dans le futur.";
    static final String ARRETE_FUTUR = "La date de publication de l'arrêté ne peut pas être dans le futur.";
    static final String ETAPE_INCONNUE = "Cette étape de la déclaration n'existe pas.";
    static final String NOMBRE_BLESSES = "Indiquez un nombre de blessés entre 1 et 99.";
    static final String DECLARANT_INCONNU = "Indiquez qui déclare le sinistre parmi les propositions.";
    static final String REPONSES_TROP_NOMBREUSES = "Trop de réponses ont été envoyées pour ce type de sinistre.";
    static final String DATE_MANQUANTE = "Indiquez la date du sinistre.";
    static final String LIEU_MANQUANT = "Indiquez le lieu du sinistre.";
    static final String DESCRIPTION_MANQUANTE = "Décrivez ce qui s'est passé.";
    static final String TIERS_MANQUANT = "Indiquez si un tiers est impliqué.";
    static final String BLESSES_MANQUANT = "Indiquez s'il y a des blessés.";
    static final String NOMBRE_BLESSES_MANQUANT = "Indiquez le nombre de blessés.";
    static final String DECLARANT_MANQUANT = "Indiquez qui déclare le sinistre.";
    static final String IDENTITE_DECLARANT = "Indiquez votre nom et votre lien avec l'assuré.";
    static final String EXPLICATION_RETARD = "Expliquez en quelques mots la raison du délai de déclaration.";
    static final String HONNEUR = "Cochez la déclaration sur l'honneur pour envoyer votre déclaration.";

    static final int MAX_COURT = 100;
    static final int MAX_LIEU = 255;
    static final int MAX_REPONSE = 500;
    static final int MAX_EXPLICATION = 2000;
    static final int MAX_DESCRIPTION = 4000;
    static final int MAX_DETAILS = 20;
    static final int MAX_BLESSES = 99;
    static final int DERNIERE_ETAPE = 3;

    private ValidationDeclaration() {
    }

    /** À chaque enregistrement du brouillon. */
    public static void verifierReponses(MiseAJourSinistreRequest demande, Set<String> types, LocalDate aujourdhui) {
        exiger(demande.type() != null && types.contains(demande.type()), TYPE_INCONNU);
        exiger(demande.etape() == null || demande.etape() >= 1 && demande.etape() <= DERNIERE_ETAPE, ETAPE_INCONNUE);
        exiger(demande.dateSurvenance() == null || !demande.dateSurvenance().isAfter(aujourdhui), DATE_FUTURE);
        exiger(demande.dateArreteCatnat() == null || !demande.dateArreteCatnat().isAfter(aujourdhui), ARRETE_FUTUR);
        exiger(demande.nombreBlesses() == null
                || demande.nombreBlesses() >= 1 && demande.nombreBlesses() <= MAX_BLESSES, NOMBRE_BLESSES);
        exiger(demande.declarant() == null || DECLARANTS.contains(demande.declarant()), DECLARANT_INCONNU);
        longueur(demande.lieu(), MAX_LIEU, "Le lieu");
        longueur(demande.description(), MAX_DESCRIPTION, "La description");
        longueur(demande.explicationRetard(), MAX_EXPLICATION, "L'explication du retard");
        longueur(demande.declarantNom(), MAX_COURT, "Votre nom");
        longueur(demande.declarantLien(), MAX_COURT, "Votre lien avec l'assuré");
        verifierTiers(demande.tiers());
        verifierDetails(demande.details());
    }

    /** À l'envoi : réponses obligatoires (AC4, AC6, AC11, AC13) et déclaration sur l'honneur (AC9). */
    public static void verifierComplete(Sinistre sinistre, String nomDeclarant, String lienDeclarant,
            boolean horsDelai, Boolean declarationHonneur) {
        exiger(sinistre.getDateSurvenance() != null, DATE_MANQUANTE);
        exiger(rempli(sinistre.getLieu()), LIEU_MANQUANT);
        exiger(rempli(sinistre.getDescription()), DESCRIPTION_MANQUANTE);
        exiger(sinistre.getTiersImplique() != null, TIERS_MANQUANT);
        exiger(sinistre.getBlesses() != null, BLESSES_MANQUANT);
        exiger(!Boolean.TRUE.equals(sinistre.getBlesses()) || sinistre.getNombreBlesses() != null,
                NOMBRE_BLESSES_MANQUANT);
        exiger(sinistre.getDeclarant() != null, DECLARANT_MANQUANT);
        exiger(SOUSCRIPTEUR.equals(sinistre.getDeclarant()) || rempli(nomDeclarant) && rempli(lienDeclarant),
                IDENTITE_DECLARANT);
        exiger(!horsDelai || rempli(sinistre.getExplicationRetard()), EXPLICATION_RETARD);
        exiger(Boolean.TRUE.equals(declarationHonneur), HONNEUR);
    }

    /** Texte nettoyé : vide devient null. */
    public static String nettoyer(String texte) {
        return texte == null || texte.isBlank() ? null : texte.trim();
    }

    private static void verifierTiers(MiseAJourSinistreRequest.Tiers tiers) {
        if (tiers != null) {
            longueur(tiers.nom(), MAX_COURT, "Le nom du tiers");
            longueur(tiers.immatriculation(), MAX_COURT, "L'immatriculation du tiers");
            longueur(tiers.assureur(), MAX_COURT, "L'assureur du tiers");
        }
    }

    private static void verifierDetails(Map<String, String> details) {
        if (details == null) {
            return;
        }
        exiger(details.size() <= MAX_DETAILS, REPONSES_TROP_NOMBREUSES);
        details.forEach((question, reponse) -> {
            longueur(question, MAX_COURT, "Une question");
            longueur(reponse, MAX_REPONSE, "Une réponse");
        });
    }

    private static void longueur(String texte, int maximum, String champ) {
        exiger(texte == null || texte.length() <= maximum,
                champ + " ne doit pas dépasser " + maximum + " caractères.");
    }

    private static boolean rempli(String texte) {
        return texte != null && !texte.isBlank();
    }

    private static void exiger(boolean condition, String message) {
        if (!condition) {
            throw new DeclarationInvalideException(message);
        }
    }
}
