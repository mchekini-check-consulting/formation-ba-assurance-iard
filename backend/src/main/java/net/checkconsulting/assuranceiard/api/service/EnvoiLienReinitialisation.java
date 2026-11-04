package net.checkconsulting.assuranceiard.api.service;

/** Remet le lien de réinitialisation du mot de passe au client. */
public interface EnvoiLienReinitialisation {

    void envoyer(String email, String lien);
}
