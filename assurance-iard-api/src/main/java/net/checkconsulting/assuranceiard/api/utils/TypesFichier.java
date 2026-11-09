package net.checkconsulting.assuranceiard.api.utils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** RG4 de l'US-13 : le vrai type d'un fichier se lit dans ses premiers octets, pas dans son nom. */
public final class TypesFichier {

    public static final long TAILLE_MAX = 5L * 1024 * 1024;

    private static final Set<String> EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final byte[] SIGNATURE_PDF = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] SIGNATURE_JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] SIGNATURE_PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private TypesFichier() {
    }

    public static boolean extensionAcceptee(String nomFichier) {
        if (nomFichier == null) {
            return false;
        }
        String extension = nomFichier.substring(nomFichier.lastIndexOf('.') + 1);
        return EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    /** Renvoie pdf, jpg ou png selon le contenu réel, vide pour tout autre contenu. */
    public static Optional<String> typeReel(byte[] contenu) {
        if (commencePar(contenu, SIGNATURE_PDF)) {
            return Optional.of("pdf");
        }
        if (commencePar(contenu, SIGNATURE_JPG)) {
            return Optional.of("jpg");
        }
        if (commencePar(contenu, SIGNATURE_PNG)) {
            return Optional.of("png");
        }
        return Optional.empty();
    }

    private static boolean commencePar(byte[] contenu, byte[] signature) {
        return contenu.length >= signature.length
                && Arrays.equals(Arrays.copyOf(contenu, signature.length), signature);
    }
}
