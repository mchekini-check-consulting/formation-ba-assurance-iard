package net.checkconsulting.assuranceiard.api.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.LienReinitialisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface LienReinitialisationRepository extends JpaRepository<LienReinitialisation, Long> {

    /** Verrouille le lien : deux utilisations simultanées ne peuvent pas passer toutes les deux (RG2). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LienReinitialisation> findByJetonHash(String jetonHash);

    void deleteByCompte(Compte compte);
}
