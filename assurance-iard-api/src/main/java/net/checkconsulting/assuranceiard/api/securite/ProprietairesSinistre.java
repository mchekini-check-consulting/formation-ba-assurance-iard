package net.checkconsulting.assuranceiard.api.securite;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.repository.SinistreRepository;
import org.springframework.stereotype.Component;

/**
 * US-24 : une déclaration de sinistre (et ses pièces jointes) appartient au client propriétaire du contrat déclaré
 * (contrat.compte_id). Un identifiant qui n'est pas un nombre ne désigne aucun sinistre.
 */
@Component
@RequiredArgsConstructor
public class ProprietairesSinistre implements RessourceProtegee {

    private final SinistreRepository repository;

    @Override
    public TypeRessource type() {
        return TypeRessource.SINISTRE;
    }

    @Override
    public Set<Long> proprietaires(String identifiant) {
        if (identifiant == null || !identifiant.matches("\\d{1,18}")) {
            return Set.of();
        }
        return Set.copyOf(repository.proprietaires(Long.valueOf(identifiant)));
    }
}
