package net.checkconsulting.moteurtarifaire.api.repository;

import net.checkconsulting.moteurtarifaire.api.model.RefOption;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link RefOption} (requêtes ajoutées avec le tarif auto). */
public interface RefOptionRepository extends JpaRepository<RefOption, Long> {
}
