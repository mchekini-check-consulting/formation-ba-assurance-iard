package net.checkconsulting.assuranceiard.api.repository;

import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.model.TarifDevis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarifDevisRepository extends JpaRepository<TarifDevis, Long> {

    Optional<TarifDevis> findByReferenceDevis(UUID referenceDevis);
}
