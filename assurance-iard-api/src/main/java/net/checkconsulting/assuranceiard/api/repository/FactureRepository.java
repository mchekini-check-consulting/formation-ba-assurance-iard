package net.checkconsulting.assuranceiard.api.repository;

import java.util.Collection;
import java.util.List;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Facture;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link Facture} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface FactureRepository extends JpaRepository<Facture, Long> {

    /** US-18, AC1 : les 3 factures les plus récentes de ces contrats (hypothèse à valider PO). */
    @LogMetier
    List<Facture> findTop3ByContratIdInOrderByPeriodeFinDescIdDesc(Collection<Long> contrats);
}
