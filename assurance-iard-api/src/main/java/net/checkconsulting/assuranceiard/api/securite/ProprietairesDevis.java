package net.checkconsulting.assuranceiard.api.securite;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.repository.DevisEnregistreRepository;
import org.springframework.stereotype.Component;

/** US-04 : un devis enregistré appartient au(x) compte(s) qui l'ont enregistré. */
@Component
@RequiredArgsConstructor
public class ProprietairesDevis implements RessourceProtegee {

    private final DevisEnregistreRepository repository;

    @Override
    public TypeRessource type() {
        return TypeRessource.DEVIS;
    }

    @Override
    public Set<Long> proprietaires(String identifiant) {
        return Identifiants.uuid(identifiant).map(reference -> Set.copyOf(repository.proprietaires(reference)))
                .orElse(Set.of());
    }
}
