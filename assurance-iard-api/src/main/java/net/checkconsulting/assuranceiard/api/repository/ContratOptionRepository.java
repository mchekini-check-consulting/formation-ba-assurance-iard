package net.checkconsulting.assuranceiard.api.repository;

import net.checkconsulting.assuranceiard.api.model.ContratOption;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link ContratOption} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface ContratOptionRepository extends JpaRepository<ContratOption, Long> {
}
