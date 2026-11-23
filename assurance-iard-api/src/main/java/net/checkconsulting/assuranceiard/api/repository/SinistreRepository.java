package net.checkconsulting.assuranceiard.api.repository;

import net.checkconsulting.assuranceiard.api.model.Sinistre;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link Sinistre} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface SinistreRepository extends JpaRepository<Sinistre, Long> {
}
