package net.checkconsulting.assuranceiard.api.repository;

import java.util.Optional;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContratRepository extends JpaRepository<Contrat, Long> {

    @LogMetier
    Optional<Contrat> findByDossier(DossierSouscription dossier);

    /** RG1 : prochain numéro de séquence pour l'année donnée. */
    @LogMetier
    @Query(value = """
            SELECT COALESCE(MAX(CAST(SPLIT_PART(numero, '-', 3) AS BIGINT)), 0) + 1
            FROM contrat
            WHERE LEFT(numero, 7) = :prefixe
            """, nativeQuery = true)
    long prochainNumeroAnnee(@Param("prefixe") String prefixe);
}
