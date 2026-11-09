package net.checkconsulting.moteurtarifaire.api.repository;

import java.util.List;
import net.checkconsulting.moteurtarifaire.api.model.ParametreTarifaire;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParametreTarifaireRepository extends JpaRepository<ParametreTarifaire, Long> {

    List<ParametreTarifaire> findByGrilleId(Long grilleId);
}
