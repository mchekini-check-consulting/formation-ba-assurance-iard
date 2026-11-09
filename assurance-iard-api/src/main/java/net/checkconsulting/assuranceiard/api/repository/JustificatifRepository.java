package net.checkconsulting.assuranceiard.api.repository;

import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.model.Justificatif;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JustificatifRepository extends JpaRepository<Justificatif, Long> {

    Optional<Justificatif> findByDossierAndCategorie(DossierSouscription dossier, String categorie);

    List<Justificatif> findByDossier(DossierSouscription dossier);
}
