package net.checkconsulting.moteurtarifaire.api.repository;

import net.checkconsulting.moteurtarifaire.api.model.RefFormule;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link RefFormule} (requêtes ajoutées avec le tarif auto). */
public interface RefFormuleRepository extends JpaRepository<RefFormule, Long> {
}
