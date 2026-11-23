package net.checkconsulting.assuranceiard.api.repository;

import net.checkconsulting.assuranceiard.api.model.RefPlaqueTest;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link RefPlaqueTest} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface RefPlaqueTestRepository extends JpaRepository<RefPlaqueTest, Long> {
}
