package net.checkconsulting.assuranceiard.api.repository;

import net.checkconsulting.assuranceiard.api.model.RefVehicule;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link RefVehicule} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface RefVehiculeRepository extends JpaRepository<RefVehicule, Long> {
}
