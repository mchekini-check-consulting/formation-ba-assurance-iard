package net.checkconsulting.assuranceiard.api.dto;

import java.util.UUID;

/** US-04 : un document déposé par le client (US-13), sans son contenu. */
public record DocumentResponse(
        Long id,
        UUID referenceDevis,
        String categorie,
        String typeDocument,
        String nomFichier,
        String typeFichier,
        int taille) {
}
