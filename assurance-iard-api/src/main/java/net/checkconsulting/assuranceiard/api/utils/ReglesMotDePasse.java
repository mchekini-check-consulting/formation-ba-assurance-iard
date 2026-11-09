package net.checkconsulting.assuranceiard.api.utils;

/** Règles communes à la création du compte (US-05) et à la réinitialisation (US-07). */
public final class ReglesMotDePasse {

    public static final String MOTIF = "^(?=.*[A-Z])(?=.*\\d).{8,}$";
    public static final String MESSAGE = "8 caractères minimum, une majuscule et un chiffre";
    public static final int LONGUEUR_MAX = 72;

    private ReglesMotDePasse() {
    }
}
