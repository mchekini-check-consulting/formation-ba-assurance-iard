package net.checkconsulting.assuranceiard.api.exception;

/** US-24, AC15 : pièce refusée (format, taille, nombre) ; le message dit pourquoi et quoi déposer. */
public class PieceRefuseeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String FORMAT = "Ce fichier n'est pas accepté : déposez un PDF, un JPG ou un PNG.";
    public static final String TAILLE = "Ce fichier dépasse 10 Mo : déposez un fichier plus léger (PDF, JPG ou PNG "
            + "de 10 Mo maximum).";
    public static final String VIDE = "Ce fichier est vide : déposez un PDF, un JPG ou un PNG.";
    public static final String NOMBRE = "Vous avez déjà déposé 20 fichiers, le maximum pour un dossier.";
    public static final String TYPE_PIECE = "Choisissez à quelle pièce attendue correspond ce fichier.";

    public PieceRefuseeException(String message) {
        super(message);
    }
}
