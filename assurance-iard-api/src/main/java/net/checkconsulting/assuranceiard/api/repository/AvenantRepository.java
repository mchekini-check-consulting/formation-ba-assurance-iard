package net.checkconsulting.assuranceiard.api.repository;

import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Avenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

/** T-05 : accès à la table de {@link Avenant}. US-23 : avenant en attente, historique, numérotation. */
public interface AvenantRepository extends JpaRepository<Avenant, Long> {

    String EN_ATTENTE = "EN_ATTENTE";
    String ACCEPTE = "ACCEPTE";

    /** US-23 : l'avenant en attente du contrat (un seul, index uk_avenant_en_attente). */
    @LogMetier
    Optional<Avenant> findFirstByContratIdAndStatut(Long contratId, String statut);

    /** US-23 : le même, verrouillé pour l'acceptation ou l'abandon (deux clics simultanés ne se croisent pas). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Avenant a WHERE a.contratId = :contratId AND a.statut = 'EN_ATTENTE'")
    Optional<Avenant> enAttenteVerrouille(@Param("contratId") Long contratId);

    /** US-23 : historique des avenants du contrat, le plus récent d'abord. */
    @LogMetier
    List<Avenant> findByContratIdOrderByNumeroDesc(Long contratId);

    /** US-23, RG3 : avenants acceptés du contrat, par date d'effet (échéancier). */
    @LogMetier
    List<Avenant> findByContratIdAndStatutOrderByDateEffetAscIdAsc(Long contratId, String statut);

    /** US-23 : rang du prochain avenant du contrat (1, 2, 3...), jamais réutilisé. */
    @Query("SELECT COALESCE(MAX(a.numero), 0) + 1 FROM Avenant a WHERE a.contratId = :contratId")
    int prochainNumero(@Param("contratId") Long contratId);

    /** US-23 : avenant d'un document (document créé avant US-23, sans référence). */
    Optional<Avenant> findFirstByDocumentId(Long documentId);
}
