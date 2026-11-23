package net.checkconsulting.assuranceiard.api.repository;

import net.checkconsulting.assuranceiard.api.model.HistoriqueStatutContrat;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link HistoriqueStatutContrat} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface HistoriqueStatutContratRepository extends JpaRepository<HistoriqueStatutContrat, Long> {
}
