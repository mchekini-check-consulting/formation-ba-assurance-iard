package net.checkconsulting.assuranceiard.api.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DevisEnregistre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DevisEnregistreRepository extends JpaRepository<DevisEnregistre, Long> {

    @LogMetier
    Optional<DevisEnregistre> findByCompteAndReference(Compte compte, UUID reference);

    @LogMetier
    List<DevisEnregistre> findByCompteOrderByDateCreationDesc(Compte compte);

    Optional<DevisEnregistre> findFirstByReferenceOrderByDateCreationDesc(UUID reference);

    @LogMetier
    long deleteByDateDernierContactBefore(Instant limite);

    /** US-04 : comptes propriétaires du devis. */
    @Query("SELECT d.compte.id FROM DevisEnregistre d WHERE d.reference = :reference")
    List<Long> proprietaires(@Param("reference") UUID reference);
}
