package net.checkconsulting.assuranceiard.api.repository;

import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommandationDevisRepository extends JpaRepository<RecommandationDevis, Long> {

    @LogMetier
    Optional<RecommandationDevis> findByReference(UUID reference);
}
