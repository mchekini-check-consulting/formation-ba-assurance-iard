package net.checkconsulting.assuranceiard.api.repository;

import net.checkconsulting.assuranceiard.api.model.AvenantLigne;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link AvenantLigne} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface AvenantLigneRepository extends JpaRepository<AvenantLigne, Long> {
}
