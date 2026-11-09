package net.checkconsulting.assuranceiard.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages de la connexion.
 *
 * @param codeBouchonActif RG4 de l'US-06 et bouchon de l'US-07 : le code 123456 est accepté et le lien de
 *     réinitialisation est écrit dans les logs. Activé par CODE_BOUCHON_ACTIF (local, dev et qualif),
 *     à retirer dès qu'un envoi réel existe.
 * @param urlFrontend adresse de l'écran, pour construire le lien de réinitialisation (FRONTEND_URL).
 */
@ConfigurationProperties("assurance.connexion")
public record ConnexionProperties(boolean codeBouchonActif, String urlFrontend) {
}
