package net.checkconsulting.assuranceiard.api.repository;

import net.checkconsulting.assuranceiard.api.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-05 : accès à la table de {@link Document} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface DocumentRepository extends JpaRepository<Document, Long> {
}
