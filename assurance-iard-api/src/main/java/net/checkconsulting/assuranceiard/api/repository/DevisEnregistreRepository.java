package net.checkconsulting.assuranceiard.api.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DevisEnregistre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevisEnregistreRepository extends JpaRepository<DevisEnregistre, Long> {

    @LogMetier
    Optional<DevisEnregistre> findByCompteAndReference(Compte compte, UUID reference);

    @LogMetier
    List<DevisEnregistre> findByCompteOrderByDateCreationDesc(Compte compte);

    Optional<DevisEnregistre> findFirstByReferenceOrderByDateCreationDesc(UUID reference);

    @LogMetier
    long deleteByDateDernierContactBefore(Instant limite);
}
