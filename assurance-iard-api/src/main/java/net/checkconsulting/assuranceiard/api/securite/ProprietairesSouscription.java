package net.checkconsulting.assuranceiard.api.securite;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.repository.DossierSouscriptionRepository;
import org.springframework.stereotype.Component;

/** US-04 : un dossier de souscription appartient au compte qui l'a ouvert. */
@Component
@RequiredArgsConstructor
public class ProprietairesSouscription implements RessourceProtegee {

    private final DossierSouscriptionRepository repository;

    @Override
    public TypeRessource type() {
        return TypeRessource.SOUSCRIPTION;
    }

    @Override
    public Set<Long> proprietaires(String identifiant) {
        return Identifiants.uuid(identifiant).map(reference -> Set.copyOf(repository.proprietaires(reference)))
                .orElse(Set.of());
    }
}
