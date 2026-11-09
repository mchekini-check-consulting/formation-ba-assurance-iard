package net.checkconsulting.assuranceiard.api.repository;

import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.CodeSignatureContrat;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeSignatureContratRepository extends JpaRepository<CodeSignatureContrat, Long> {

    Optional<CodeSignatureContrat> findByContrat(Contrat contrat);

    void deleteByContrat(Contrat contrat);
}
