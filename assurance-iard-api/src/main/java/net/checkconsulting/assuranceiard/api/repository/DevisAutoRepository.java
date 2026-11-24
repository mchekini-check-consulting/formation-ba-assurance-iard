package net.checkconsulting.assuranceiard.api.repository;

import java.util.Collection;
import java.util.List;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.DevisAuto;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link DevisAuto} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface DevisAutoRepository extends JpaRepository<DevisAuto, Long> {

    /** US-18 : partie auto de ces devis (véhicule). */
    @LogMetier
    List<DevisAuto> findByDevisIdIn(Collection<Long> devis);
}
