package net.checkconsulting.moteurtarifaire.api.repository;

import java.util.List;
import net.checkconsulting.moteurtarifaire.api.model.RefTarif;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefTarifRepository extends JpaRepository<RefTarif, Long> {

    List<RefTarif> findByGrilleId(Long grilleId);
}
