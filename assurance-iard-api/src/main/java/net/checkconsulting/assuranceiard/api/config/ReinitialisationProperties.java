package net.checkconsulting.assuranceiard.api.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Réglages du mot de passe oublié (US-07).
 *
 * @param validiteLien durée de validité du lien de réinitialisation (LIEN_REINITIALISATION_VALIDITE, ex. 24h,
 *     2m) : 24 heures par défaut, valeur à confirmer par la PO.
 */
@ConfigurationProperties("assurance.reinitialisation")
public record ReinitialisationProperties(@DefaultValue("24h") Duration validiteLien) {
}
