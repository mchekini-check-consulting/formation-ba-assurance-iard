package net.checkconsulting.assuranceiard.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * US-24 : réglages de la déclaration de sinistre.
 *
 * @param numeroAssistance AC10 : numéro d'assistance 24h/24 affiché en haut de chaque étape (conservé, décision PO
 *     du 27/11/2026).
 */
@ConfigurationProperties("assurance.sinistres")
public record SinistresProperties(String numeroAssistance) {
}
