package net.checkconsulting.assuranceiard.api.repository;

import net.checkconsulting.assuranceiard.api.model.Compte;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompteRepository extends JpaRepository<Compte, Long> {

    boolean existsByEmail(String email);
}
