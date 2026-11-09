package net.checkconsulting.assuranceiard.api.service;

import org.springframework.stereotype.Component;

/** RG4 : aucun envoi réel d'e-mail ni de SMS pour l'instant. Le code n'est ni envoyé ni écrit dans les logs. */
@Component
public class EnvoiCodeBouchon implements EnvoiCode {

    @Override
    public void envoyer(String email, String code) {
        // Bouchon : à remplacer par un envoi réel.
    }
}
