package net.checkconsulting.moteurtarifaire.api.repository;

import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.moteurtarifaire.api.logging.LogMetier;
import net.checkconsulting.moteurtarifaire.api.model.CalculTarif;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculTarifRepository extends JpaRepository<CalculTarif, Long> {

    @LogMetier
    Optional<CalculTarif> findByIdTarif(UUID idTarif);
}
