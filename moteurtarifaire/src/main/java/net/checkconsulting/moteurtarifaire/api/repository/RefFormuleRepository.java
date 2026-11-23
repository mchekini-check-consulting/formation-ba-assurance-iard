package net.checkconsulting.moteurtarifaire.api.repository;

import java.util.List;
import net.checkconsulting.moteurtarifaire.api.model.RefFormule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefFormuleRepository extends JpaRepository<RefFormule, Long> {

    List<RefFormule> findByGrilleIdOrderByOrdre(Long grilleId);
}
