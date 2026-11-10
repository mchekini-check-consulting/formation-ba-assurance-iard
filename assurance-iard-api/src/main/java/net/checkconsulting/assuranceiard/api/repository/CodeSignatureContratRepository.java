package net.checkconsulting.assuranceiard.api.repository;

import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.CodeSignatureContrat;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodeSignatureContratRepository extends JpaRepository<CodeSignatureContrat, Long> {

    Optional<CodeSignatureContrat> findByContrat(Contrat contrat);

    @Modifying
    @Query("DELETE FROM CodeSignatureContrat c WHERE c.contrat = :contrat")
    void deleteByContrat(@Param("contrat") Contrat contrat);
}
