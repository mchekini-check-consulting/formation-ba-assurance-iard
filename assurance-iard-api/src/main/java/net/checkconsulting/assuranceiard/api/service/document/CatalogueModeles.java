package net.checkconsulting.assuranceiard.api.service.document;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.checkconsulting.assuranceiard.api.model.Document;
import org.springframework.stereotype.Component;

/** T-03 : tous les {@link ModeleDocument} de l'application, enregistrés automatiquement par Spring. */
@Component
public class CatalogueModeles {

    private final List<ModeleDocument> modeles;

    public CatalogueModeles(List<ModeleDocument> modeles) {
        this.modeles = List.copyOf(modeles);
    }

    /** Le modèle qui produit ce document ; vide si aucun (type pas encore développé). */
    public Optional<ModeleDocument> pour(Document document) {
        List<ModeleDocument> candidats = modeles.stream()
                .filter(modele -> modele.type().name().equals(document.getType()))
                .filter(modele -> modele.convientA(document))
                .toList();
        if (candidats.size() > 1) {
            throw new IllegalStateException("Plusieurs modèles pour le type " + document.getType());
        }
        return candidats.stream().findFirst();
    }

    /** Types de document qu'au moins un modèle sait produire (les seuls que la reprise traite). */
    public Set<String> typesGeres() {
        return modeles.stream().map(modele -> modele.type().name()).collect(Collectors.toUnmodifiableSet());
    }
}
