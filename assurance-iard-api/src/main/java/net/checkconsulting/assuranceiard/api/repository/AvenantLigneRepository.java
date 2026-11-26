package net.checkconsulting.assuranceiard.api.repository;

import java.util.Collection;
import java.util.List;
import net.checkconsulting.assuranceiard.api.model.AvenantLigne;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link AvenantLigne}. US-23 : garanties ajoutées ou retirées par un avenant. */
public interface AvenantLigneRepository extends JpaRepository<AvenantLigne, Long> {

    List<AvenantLigne> findByAvenantIdOrderById(Long avenantId);

    List<AvenantLigne> findByAvenantIdInOrderById(Collection<Long> avenantIds);
}
