package net.checkconsulting.assuranceiard.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages de la connexion.
 *
 * @param codeBouchonActif bouchon de l'US-07 : lien de réinitialisation du mot de passe écrit dans les logs.
 *     Activé par CODE_BOUCHON_ACTIF (local, dev et qualif), à retirer dès qu'un envoi réel existe.
 *     Le code de connexion (US-06) a son propre réglage : {@link DoubleAuthentificationProperties}.
 * @param urlFrontend adresse de l'écran, pour construire le lien de réinitialisation (FRONTEND_URL).
 */
@ConfigurationProperties("assurance.connexion")
public record ConnexionProperties(boolean codeBouchonActif, String urlFrontend) {
}
