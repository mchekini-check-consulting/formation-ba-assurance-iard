package net.checkconsulting.assuranceiard.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContratRepository extends JpaRepository<Contrat, Long> {

    @LogMetier
    Optional<Contrat> findByDossier(DossierSouscription dossier);

    /** US-04 : contrats d'un client, les plus récents d'abord. */
    @LogMetier
    List<Contrat> findByDossierCompteOrderByDateCreationDesc(Compte compte);

    /** US-04 : comptes propriétaires du contrat de ce devis. */
    @Query("SELECT c.dossier.compte.id FROM Contrat c WHERE c.dossier.referenceDevis = :referenceDevis")
    List<Long> proprietaires(@Param("referenceDevis") UUID referenceDevis);

    /** RG1 : prochain numéro de séquence pour l'année donnée. */
    @LogMetier
    @Query(value = """
            SELECT COALESCE(MAX(CAST(SPLIT_PART(numero, '-', 3) AS BIGINT)), 0) + 1
            FROM contrat
            WHERE LEFT(numero, 7) = :prefixe
            """, nativeQuery = true)
    long prochainNumeroAnnee(@Param("prefixe") String prefixe);
}
