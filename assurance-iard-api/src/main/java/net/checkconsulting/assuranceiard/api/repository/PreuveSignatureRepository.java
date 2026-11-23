package net.checkconsulting.assuranceiard.api.repository;

import net.checkconsulting.assuranceiard.api.model.PreuveSignature;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link PreuveSignature} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface PreuveSignatureRepository extends JpaRepository<PreuveSignature, Long> {
}
