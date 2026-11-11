package net.checkconsulting.assuranceiard.api.securite;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import org.springframework.stereotype.Component;

/** US-04 : un contrat appartient au client de son dossier de souscription. */
@Component
@RequiredArgsConstructor
public class ProprietairesContrat implements RessourceProtegee {

    private final ContratRepository repository;

    @Override
    public TypeRessource type() {
        return TypeRessource.CONTRAT;
    }

    @Override
    public Set<Long> proprietaires(String identifiant) {
        return Identifiants.uuid(identifiant).map(reference -> Set.copyOf(repository.proprietaires(reference)))
                .orElse(Set.of());
    }
}
