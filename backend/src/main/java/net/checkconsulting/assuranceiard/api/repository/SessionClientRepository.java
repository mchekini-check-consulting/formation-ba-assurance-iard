package net.checkconsulting.assuranceiard.api.repository;

import java.time.Instant;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.SessionClient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionClientRepository extends JpaRepository<SessionClient, Long> {

    Optional<SessionClient> findByJetonHash(String jetonHash);

    void deleteByJetonHash(String jetonHash);

    void deleteByCompteAndDateExpirationBefore(Compte compte, Instant date);
}
