package net.checkconsulting.assuranceiard.api.repository;

import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.PieceJointe;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 / US-24 : accès à la table de {@link PieceJointe}. */
public interface PieceJointeRepository extends JpaRepository<PieceJointe, Long> {

    /** US-24, AC7 : pièces d'un dossier, dans l'ordre du dépôt. */
    List<PieceJointe> findBySinistreIdAndStatutOrderByIdAsc(Long sinistreId, String statut);

    /** US-24, RG7 : nombre de pièces d'un dossier (20 au plus). */
    long countBySinistreIdAndStatut(Long sinistreId, String statut);

    Optional<PieceJointe> findByIdAndSinistreId(Long id, Long sinistreId);
}
