package net.checkconsulting.assuranceiard.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages de la connexion.
 *
 * @param codeBouchonActif bouchon de la signature de contrat : code de signature 123456 accepté. Activé par
 *     CODE_BOUCHON_ACTIF (local, dev et qualif), à retirer dès qu'un envoi réel existe. Le code de connexion (US-06)
 *     a son propre réglage : {@link DoubleAuthentificationProperties}.
 * @param urlFrontend adresse de l'écran de l'environnement (FRONTEND_URL), base du lien de réinitialisation envoyé
 *     par e-mail (US-07).
 */
@ConfigurationProperties("assurance.connexion")
public record ConnexionProperties(boolean codeBouchonActif, String urlFrontend) {
}
