package net.checkconsulting.assuranceiard.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.config.ConnexionProperties;
import net.checkconsulting.assuranceiard.api.utils.MasqueEmail;
import org.springframework.stereotype.Component;

/**
 * Bouchon de l'US-07 : aucun e-mail réel, le lien est écrit dans la console du backend. Ce lien donne accès
 * au compte : il n'est jamais écrit quand le bouchon est inactif (preprod, prod). À remplacer par un envoi réel.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvoiLienBouchon implements EnvoiLienReinitialisation {

    private final ConnexionProperties proprietes;

    @Override
    public void envoyer(String email, String lien) {
        if (proprietes.codeBouchonActif()) {
            log.info("Bouchon : lien de réinitialisation pour {} : {}", MasqueEmail.masquer(email), lien);
        }
    }
}
