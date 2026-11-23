package net.checkconsulting.assuranceiard.api.repository;

import java.util.Optional;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.DocumentContenu;
import org.springframework.data.jpa.repository.JpaRepository;

/** T-03 : PDF des documents générés. */
public interface DocumentContenuRepository extends JpaRepository<DocumentContenu, Long> {

    @LogMetier
    Optional<DocumentContenu> findByDocumentId(Long documentId);
}
