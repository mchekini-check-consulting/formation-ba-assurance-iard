package net.checkconsulting.assuranceiard.api.exception;

/** US-23 : aucun avenant en attente sur ce contrat (déjà accepté, abandonné ou jamais préparé). */
public class AvenantInconnuException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AvenantInconnuException() {
        super("Aucun avenant n'attend votre réponse sur ce contrat.");
    }
}
