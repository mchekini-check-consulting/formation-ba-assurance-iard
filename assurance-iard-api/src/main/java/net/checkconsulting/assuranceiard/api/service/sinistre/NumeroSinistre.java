package net.checkconsulting.assuranceiard.api.service.sinistre;

import java.time.Instant;
import java.time.ZoneId;

/**
 * US-24, RG5 : numéro de dossier SIN-AAAA-XXXXXX, sur le modèle des factures (FAC-AAAA-NNNNNN) : année de l'envoi
 * (heure de Paris) et identifiant du sinistre sur 6 chiffres, qui ne revient jamais en arrière. Unique (contrainte
 * uk_sinistre_numero).
 */
public final class NumeroSinistre {

    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");

    private NumeroSinistre() {
    }

    public static String de(Instant envoi, long identifiant) {
        return "SIN-%d-%06d".formatted(envoi.atZone(FRANCE).getYear(), identifiant);
    }
}
