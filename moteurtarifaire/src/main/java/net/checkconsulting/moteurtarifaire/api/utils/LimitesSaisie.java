package net.checkconsulting.moteurtarifaire.api.utils;

public final class LimitesSaisie {

    public static final String SURFACE_MIN = "9";
    public static final String SURFACE_MAX = "1000";
    public static final int PIECES_MIN = 1;
    public static final int PIECES_MAX = 5;
    public static final int SINISTRES_MIN = 0;
    public static final int SINISTRES_MAX = 3;
    public static final String CONTENU_MIN = "0";
    public static final String FORMAT_CODE_POSTAL = "\\d{5}";

    private LimitesSaisie() {
    }
}
