package net.checkconsulting.assuranceiard.api.repository;

import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.model.Justificatif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JustificatifRepository extends JpaRepository<Justificatif, Long> {

    Optional<Justificatif> findByDossierAndCategorie(DossierSouscription dossier, String categorie);

    List<Justificatif> findByDossier(DossierSouscription dossier);

    /** US-04 : documents d'un client. */
    List<Justificatif> findByDossierCompteOrderByIdDesc(Compte compte);

    /** US-04 : compte propriétaire du document. */
    @Query("SELECT j.dossier.compte.id FROM Justificatif j WHERE j.id = :id")
    List<Long> proprietaires(@Param("id") Long id);
}
