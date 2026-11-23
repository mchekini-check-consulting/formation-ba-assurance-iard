package net.checkconsulting.assuranceiard.api.repository;

import net.checkconsulting.assuranceiard.api.model.PieceJointe;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link PieceJointe} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface PieceJointeRepository extends JpaRepository<PieceJointe, Long> {
}
