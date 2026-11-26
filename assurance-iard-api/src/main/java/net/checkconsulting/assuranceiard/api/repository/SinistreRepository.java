package net.checkconsulting.assuranceiard.api.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Sinistre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** T-05 / US-24 : accès à la table de {@link Sinistre}. */
public interface SinistreRepository extends JpaRepository<Sinistre, Long> {

    /**
     * US-18, AC1 et US-24 : tableau de bord et « Mes sinistres » : dossiers déclarés et brouillons pas encore expirés de ces contrats, les plus récents
     * d'abord (un brouillon expiré n'a plus aucune valeur, RG6, même avant son passage à la purge).
     */
    @LogMetier
    @Query("""
            SELECT s FROM Sinistre s
            WHERE s.contratId IN :contrats AND (s.statut = 'DECLARE' OR s.brouillonExpireLe > :maintenant)
            ORDER BY s.dateModification DESC, s.id DESC""")
    List<Sinistre> visibles(@Param("contrats") Collection<Long> contrats, @Param("maintenant") Instant maintenant);

    /** US-24 : compte propriétaire du contrat de ce sinistre (contrôle d'accès d'US-04). */
    @Query("SELECT c.compte.id FROM Sinistre s, Contrat c WHERE c.id = s.contratId AND s.id = :id")
    List<Long> proprietaires(@Param("id") Long id);
}
