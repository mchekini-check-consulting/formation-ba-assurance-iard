package net.checkconsulting.assuranceiard.api.repository;

import net.checkconsulting.assuranceiard.api.model.Conducteur;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link Conducteur} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface ConducteurRepository extends JpaRepository<Conducteur, Long> {
}
