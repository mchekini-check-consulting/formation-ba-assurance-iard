package net.checkconsulting.assuranceiard.api.repository;

import net.checkconsulting.assuranceiard.api.model.DevisOption;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link DevisOption} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface DevisOptionRepository extends JpaRepository<DevisOption, Long> {
}
