package net.checkconsulting.assuranceiard.api.repository;

import java.util.List;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Echeance;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link Echeance} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface EcheanceRepository extends JpaRepository<Echeance, Long> {

    /** US-22, AC1 : échéances enregistrées d'un contrat (données de test), la plus récente d'abord. */
    @LogMetier
    List<Echeance> findByContratIdOrderByDateEcheanceDescIdDesc(Long contratId);

    /** US-31 étape 5 : échéances d'un contrat auto, par ordre chronologique (PDF + affichage). */
    @LogMetier
    List<Echeance> findByContratIdOrderByDateEcheanceAscIdAsc(Long contratId);
}
