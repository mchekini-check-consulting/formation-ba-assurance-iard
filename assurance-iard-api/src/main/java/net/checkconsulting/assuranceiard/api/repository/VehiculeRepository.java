package net.checkconsulting.assuranceiard.api.repository;

import net.checkconsulting.assuranceiard.api.model.Vehicule;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link Vehicule} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface VehiculeRepository extends JpaRepository<Vehicule, Long> {
}
