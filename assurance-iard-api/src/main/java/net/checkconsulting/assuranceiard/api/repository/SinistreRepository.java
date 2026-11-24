package net.checkconsulting.assuranceiard.api.repository;

import java.util.Collection;
import java.util.List;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Sinistre;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link Sinistre} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface SinistreRepository extends JpaRepository<Sinistre, Long> {

    /** US-18, AC1 : sinistres de ces contrats dans ce statut, les plus récents d'abord. */
    @LogMetier
    List<Sinistre> findByContratIdInAndStatutOrderByDateSurvenanceDescIdDesc(Collection<Long> contrats,
            String statut);
}
