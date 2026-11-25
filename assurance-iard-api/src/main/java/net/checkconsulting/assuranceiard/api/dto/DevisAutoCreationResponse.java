package net.checkconsulting.assuranceiard.api.dto;

/** US-28 / US-59 : réponse à la création d'un devis auto (étape 1.4). */
public record DevisAutoCreationResponse(
        Long devisAutoId,
        String statutDevis,
        boolean eligibleEnLigne) {
}
