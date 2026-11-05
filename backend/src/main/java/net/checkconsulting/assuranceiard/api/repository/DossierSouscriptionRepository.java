package net.checkconsulting.assuranceiard.api.repository;

import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DossierSouscriptionRepository extends JpaRepository<DossierSouscription, Long> {

    Optional<DossierSouscription> findByCompteAndReferenceDevis(Compte compte, UUID referenceDevis);
}
