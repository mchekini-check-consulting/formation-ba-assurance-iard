package net.checkconsulting.moteurtarifaire.api.repository;

import net.checkconsulting.moteurtarifaire.api.model.RefTarif;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link RefTarif} (requêtes ajoutées avec le tarif auto). */
public interface RefTarifRepository extends JpaRepository<RefTarif, Long> {
}
