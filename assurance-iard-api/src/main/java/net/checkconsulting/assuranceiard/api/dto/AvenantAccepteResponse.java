package net.checkconsulting.assuranceiard.api.dto;

/**
 * US-23, AC4 et AC7 : avenant accepté.
 *
 * @param disponible vrai si le PDF de l'avenant est déjà produit (sinon la reprise de T-03 s'en charge)
 * @param emailEnvoye vrai si l'avenant est parti par e-mail
 * @param versionConditionsParticulieres AC7 : nouvelle version des conditions particulières ; vide si elle n'a pas
 *     pu être créée (elle le sera par la reprise)
 */
public record AvenantAccepteResponse(
        AvenantResponse avenant,
        boolean disponible,
        boolean emailEnvoye,
        Integer versionConditionsParticulieres) {
}
