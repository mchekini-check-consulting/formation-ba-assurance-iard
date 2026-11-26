package net.checkconsulting.assuranceiard.api.repository;

import java.time.LocalDate;
import java.util.List;
import net.checkconsulting.assuranceiard.api.model.ContratOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** T-05 : accès à la table de {@link ContratOption}. */
public interface ContratOptionRepository extends JpaRepository<ContratOption, Long> {

    /** US-23 : options souscrites et pas encore retirées à cette date (date de fin vide ou postérieure). */
    @Query("""
            SELECT o FROM ContratOption o
            WHERE o.contratId = :contratId AND (o.dateFin IS NULL OR o.dateFin >= :date)
            ORDER BY o.id""")
    List<ContratOption> enCours(@Param("contratId") Long contratId, @Param("date") LocalDate date);

    /**
     * US-24, RG4 : options en vigueur à cette date (souscrites au plus tard ce jour-là et pas encore retirées), y
     * compris celles ajoutées ou retirées par un avenant d'US-23.
     */
    @Query("""
            SELECT o FROM ContratOption o
            WHERE o.contratId = :contratId AND o.dateDebut <= :date AND (o.dateFin IS NULL OR o.dateFin >= :date)
            ORDER BY o.id""")
    List<ContratOption> aLaDate(@Param("contratId") Long contratId, @Param("date") LocalDate date);

    /** US-23 : vrai si les options du contrat sont déjà enregistrées (sinon, contrat du sprint 1 : celles du devis). */
    boolean existsByContratId(Long contratId);
}
