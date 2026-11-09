package net.checkconsulting.assuranceiard.api.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Valeurs aléatoires de la connexion. Seule l'empreinte d'un jeton est stockée en base. */
public final class Jetons {

    private static final SecureRandom HASARD = new SecureRandom();
    private static final int OCTETS_JETON = 32;
    private static final int PLAGE_CODE = 1_000_000;

    private Jetons() {
    }

    public static String nouveauJeton() {
        byte[] octets = new byte[OCTETS_JETON];
        HASARD.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    public static String nouveauCode() {
        return String.format("%06d", HASARD.nextInt(PLAGE_CODE));
    }

    public static String empreinte(String jeton) {
        try {
            byte[] octets = MessageDigest.getInstance("SHA-256").digest(jeton.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(octets);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponible", exception);
        }
    }
}
