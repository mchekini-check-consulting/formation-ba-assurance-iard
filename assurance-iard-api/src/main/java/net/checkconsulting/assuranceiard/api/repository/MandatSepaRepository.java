package net.checkconsulting.assuranceiard.api.repository;

import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.model.MandatSepa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MandatSepaRepository extends JpaRepository<MandatSepa, Long> {

    Optional<MandatSepa> findByDossier(DossierSouscription dossier);
}
