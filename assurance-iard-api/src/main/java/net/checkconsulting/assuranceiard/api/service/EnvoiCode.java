package net.checkconsulting.assuranceiard.api.service;

/** Remet le code de vérification au client. */
public interface EnvoiCode {

    void envoyer(String email, String code);
}
