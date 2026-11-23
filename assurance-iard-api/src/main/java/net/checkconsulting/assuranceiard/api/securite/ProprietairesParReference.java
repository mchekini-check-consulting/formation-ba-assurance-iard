package net.checkconsulting.assuranceiard.api.securite;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** US-04 : ressource identifiée par la référence du devis ; le dépôt donne les comptes propriétaires. */
abstract class ProprietairesParReference implements RessourceProtegee {

    private final TypeRessource type;
    private final Function<UUID, List<Long>> recherche;

    ProprietairesParReference(TypeRessource type, Function<UUID, List<Long>> recherche) {
        this.type = type;
        this.recherche = recherche;
    }

    @Override
    public TypeRessource type() {
        return type;
    }

    @Override
    public Set<Long> proprietaires(String identifiant) {
        return Identifiants.uuid(identifiant).map(reference -> Set.copyOf(recherche.apply(reference)))
                .orElse(Set.of());
    }
}
