package net.checkconsulting.assuranceiard.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DossierSouscriptionRepository extends JpaRepository<DossierSouscription, Long> {

    @LogMetier
    Optional<DossierSouscription> findByCompteAndReferenceDevis(Compte compte, UUID referenceDevis);

    /** US-04 : comptes propriétaires du dossier de souscription de ce devis. */
    @Query("SELECT d.compte.id FROM DossierSouscription d WHERE d.referenceDevis = :referenceDevis")
    List<Long> proprietaires(@Param("referenceDevis") UUID referenceDevis);
}
