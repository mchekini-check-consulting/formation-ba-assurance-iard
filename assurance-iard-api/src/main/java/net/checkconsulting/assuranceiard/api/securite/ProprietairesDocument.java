package net.checkconsulting.assuranceiard.api.securite;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.repository.JustificatifRepository;
import org.springframework.stereotype.Component;

/** US-04 : un document déposé appartient au client de son dossier de souscription. */
@Component
@RequiredArgsConstructor
public class ProprietairesDocument implements RessourceProtegee {

    private final JustificatifRepository repository;

    @Override
    public TypeRessource type() {
        return TypeRessource.DOCUMENT;
    }

    @Override
    public Set<Long> proprietaires(String identifiant) {
        return Identifiants.nombre(identifiant).map(id -> Set.copyOf(repository.proprietaires(id)))
                .orElse(Set.of());
    }
}
