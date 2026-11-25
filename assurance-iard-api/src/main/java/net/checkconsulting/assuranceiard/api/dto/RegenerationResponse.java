package net.checkconsulting.assuranceiard.api.dto;

import java.time.Instant;

/**
 * US-21 : conditions particulières régénérées.
 *
 * @param documentId nouvelle version, téléchargeable par /documents-contrat/{id}
 * @param disponible faux si le PDF n'a pas pu être produit tout de suite (la reprise de T-03 le stockera)
 * @param emailEnvoye vrai si le PDF a été envoyé par e-mail (AC3)
 */
public record RegenerationResponse(Long documentId, Integer version, Instant dateCreation, boolean disponible,
        boolean emailEnvoye) {
}
