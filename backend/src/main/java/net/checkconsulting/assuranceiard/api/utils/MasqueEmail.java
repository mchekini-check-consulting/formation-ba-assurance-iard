package net.checkconsulting.assuranceiard.api.utils;

/** Adresse affichée à l'écran de vérification : marie@exemple.fr devient m***@exemple.fr. */
public final class MasqueEmail {

    private MasqueEmail() {
    }

    public static String masquer(String email) {
        int arobase = email.lastIndexOf('@');
        if (arobase <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(arobase);
    }
}
