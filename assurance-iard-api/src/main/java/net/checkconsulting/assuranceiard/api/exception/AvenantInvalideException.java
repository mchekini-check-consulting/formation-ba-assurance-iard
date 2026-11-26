package net.checkconsulting.assuranceiard.api.exception;

/**
 * US-23 : demande d'avenant refusée (aucune garantie changée, garantie inconnue ou obligatoire retirée, case
 * d'acceptation non cochée). Le message s'affiche tel quel à l'écran.
 */
public class AvenantInvalideException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String AUCUN_CHANGEMENT = "Modifiez au moins une garantie pour continuer.";
    public static final String GARANTIE_INCONNUE = "Une des garanties demandées n'est pas proposée pour ce contrat.";
    public static final String ACCEPTATION_REQUISE =
            "Cochez « J'ai lu et j'accepte mon avenant » pour accepter l'avenant.";

    public AvenantInvalideException(String message) {
        super(message);
    }
}
