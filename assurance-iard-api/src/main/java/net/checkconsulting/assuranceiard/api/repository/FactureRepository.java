package net.checkconsulting.assuranceiard.api.repository;

import net.checkconsulting.assuranceiard.api.model.Facture;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link Facture} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface FactureRepository extends JpaRepository<Facture, Long> {
}
