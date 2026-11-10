package net.checkconsulting.assuranceiard.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages de la double authentification (US-06).
 *
 * @param codeTestActif code de test 123456 accepté à la connexion, en plus du vrai code envoyé par e-mail.
 *     Éteint par défaut et dans tous les environnements (CODE_TEST_CONNEXION_ACTIF) : décision finale de la PO
 *     en attente. Distinct de CODE_BOUCHON_ACTIF (US-07 et signature de contrat).
 */
@ConfigurationProperties("assurance.double-authentification")
public record DoubleAuthentificationProperties(boolean codeTestActif) {
}
