package net.checkconsulting.assuranceiard.api.repository;

import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.PieceJointeContenu;
import org.springframework.data.jpa.repository.JpaRepository;

/** US-24 : contenu des pièces jointes de sinistre (en base). */
public interface PieceJointeContenuRepository extends JpaRepository<PieceJointeContenu, Long> {

    Optional<PieceJointeContenu> findByPieceJointeId(Long pieceJointeId);
}
