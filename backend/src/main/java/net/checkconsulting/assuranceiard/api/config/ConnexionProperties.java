package net.checkconsulting.assuranceiard.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages de la connexion.
 *
 * @param codeBouchonActif RG4 : le code 123456 est accepté. Activé par CODE_BOUCHON_ACTIF
 *     (local, dev et qualif), à retirer dès qu'un envoi réel existe.
 */
@ConfigurationProperties("assurance.connexion")
public record ConnexionProperties(boolean codeBouchonActif) {
}
