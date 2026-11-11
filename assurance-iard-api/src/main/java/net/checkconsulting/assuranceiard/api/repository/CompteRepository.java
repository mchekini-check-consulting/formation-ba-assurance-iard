package net.checkconsulting.assuranceiard.api.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Profil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CompteRepository extends JpaRepository<Compte, Long> {

    boolean existsByEmail(String email);

    /** Verrouille le compte : deux tentatives simultanées ne peuvent pas fausser le compteur d'échecs. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Compte> findByEmail(String email);

    long countByProfil(Profil profil);
}
