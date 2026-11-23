package net.checkconsulting.assuranceiard.api.securite;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import org.springframework.stereotype.Component;

/**
 * T-03 : un document généré appartient au client de son contrat (ou du contrat de son sinistre, ou de son devis).
 * Les conditions générales n'ont pas de propriétaire : tout client qui a le droit de consulter ses documents peut
 * les télécharger.
 */
@Component
@RequiredArgsConstructor
public class ProprietairesDocumentContrat implements RessourceProtegee {

    private final DocumentRepository repository;

    @Override
    public TypeRessource type() {
        return TypeRessource.DOCUMENT_CONTRAT;
    }

    @Override
    public Set<Long> proprietaires(String identifiant) {
        return Identifiants.nombre(identifiant).map(id -> Set.copyOf(repository.proprietaires(id)))
                .orElse(Set.of());
    }
}
