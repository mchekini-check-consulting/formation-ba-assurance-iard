package net.checkconsulting.assuranceiard.api.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.CodeVerification;
import net.checkconsulting.assuranceiard.api.model.Compte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CodeVerificationRepository extends JpaRepository<CodeVerification, Long> {

    /** Verrouille la vérification : deux essais simultanés ne peuvent pas dépasser la limite de 3. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CodeVerification> findByJetonHash(String jetonHash);

    void deleteByCompte(Compte compte);
}
