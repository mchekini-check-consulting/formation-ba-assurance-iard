package net.checkconsulting.assuranceiard.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

/**
 * US-24, AC4 à AC13 : réponses d'une étape du brouillon, enregistrées telles quelles (toutes les réponses sont
 * envoyées à chaque fois ; une réponse vide efface la précédente). Les réponses obligatoires sont vérifiées à
 * l'envoi. Aucun champ médical (AC11).
 *
 * @param etape étape où reprendre le brouillon (1 à 3)
 * @param declarant SOUSCRIPTEUR, CONDUCTEUR (conducteur secondaire) ou PROCHE (AC13)
 * @param details réponses propres au type de sinistre (AC4), libellé de question → réponse
 */
public record MiseAJourSinistreRequest(String type, Integer etape, LocalDate dateSurvenance,
        LocalTime heureSurvenance, String lieu, String description, Boolean tiersImplique, Tiers tiers,
        Boolean blesses, Integer nombreBlesses, String declarant, String declarantNom, String declarantLien,
        Map<String, String> details, LocalDate dateArreteCatnat, String explicationRetard) {

    /** Tiers impliqué (nom, et en auto immatriculation et assureur s'ils sont connus). */
    public record Tiers(String nom, String immatriculation, String assureur) {
    }
}
