package net.checkconsulting.assuranceiard.api.repository;

import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.SouscripteurAuto;
import org.springframework.data.jpa.repository.JpaRepository;

/** US-31 : accès à la table souscripteur_auto. */
public interface SouscripteurAutoRepository extends JpaRepository<SouscripteurAuto, Long> {

    Optional<SouscripteurAuto> findByDevisAutoId(Long devisAutoId);
}
