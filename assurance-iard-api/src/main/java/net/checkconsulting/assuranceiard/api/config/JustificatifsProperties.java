package net.checkconsulting.assuranceiard.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages du dépôt de justificatifs (US-13).
 *
 * @param dossier dossier où ranger les fichiers (JUSTIFICATIFS_DOSSIER). PROTOTYPE : en ligne, dossier temporaire
 *     vidé à chaque redémarrage du backend ; à remplacer par un vrai stockage avant tout usage réel.
 */
@ConfigurationProperties("assurance.justificatifs")
public record JustificatifsProperties(String dossier) {
}
