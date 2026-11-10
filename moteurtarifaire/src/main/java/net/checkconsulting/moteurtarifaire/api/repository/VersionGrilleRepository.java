package net.checkconsulting.moteurtarifaire.api.repository;

import java.time.LocalDate;
import java.util.List;
import net.checkconsulting.moteurtarifaire.api.logging.LogMetier;
import net.checkconsulting.moteurtarifaire.api.model.VersionGrille;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VersionGrilleRepository extends JpaRepository<VersionGrille, Long> {

    @LogMetier
    @Query("""
            select v from VersionGrille v
            where v.dateDebut <= :date and (v.dateFin is null or v.dateFin >= :date)
            """)
    List<VersionGrille> findApplicablesAu(@Param("date") LocalDate date);

    List<VersionGrille> findAllByOrderByDateDebutAsc();
}
