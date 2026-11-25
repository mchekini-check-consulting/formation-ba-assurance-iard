package net.checkconsulting.assuranceiard.api.exception;

/** US-28 / US-59 : le profil conducteur n'est pas éligible à une souscription en ligne. */
public class ProfilBloqueException extends RuntimeException {

    private final String motif;

    public ProfilBloqueException(String motif, String message) {
        super(message);
        this.motif = motif;
    }

    public String getMotif() {
        return motif;
    }
}
