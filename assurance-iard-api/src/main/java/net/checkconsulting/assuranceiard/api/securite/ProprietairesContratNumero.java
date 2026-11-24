package net.checkconsulting.assuranceiard.api.securite;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import org.springframework.stereotype.Component;

/** US-18 : un contrat (habitation ou auto), identifié par son numéro, appartient au client de contrat.compte_id. */
@Component
@RequiredArgsConstructor
public class ProprietairesContratNumero implements RessourceProtegee {

    private final ContratRepository repository;

    @Override
    public TypeRessource type() {
        return TypeRessource.CONTRAT_NUMERO;
    }

    @Override
    public Set<Long> proprietaires(String identifiant) {
        return identifiant == null || identifiant.isBlank() ? Set.of()
                : Set.copyOf(repository.proprietairesParNumero(identifiant));
    }
}
