package net.checkconsulting.assuranceiard.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * US-04 : jeu de données de recette (2 assurés avec devis, contrat et document, un gestionnaire, un administrateur).
 *
 * @param actif DONNEES_TEST_ACTIVES : allumé en local, dev et qualif ; jamais en preprod ni en prod.
 * @param motDePasse mot de passe commun des comptes de recette (DONNEES_TEST_MOT_DE_PASSE).
 */
@ConfigurationProperties("assurance.donnees-test")
public record DonneesTestProperties(boolean actif, String motDePasse) {
}
