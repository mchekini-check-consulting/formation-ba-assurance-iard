package net.checkconsulting.assuranceiard.api.repository;

import net.checkconsulting.assuranceiard.api.model.Avenant;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link Avenant} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface AvenantRepository extends JpaRepository<Avenant, Long> {
}
