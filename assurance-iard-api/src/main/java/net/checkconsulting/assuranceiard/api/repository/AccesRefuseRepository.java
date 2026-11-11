package net.checkconsulting.assuranceiard.api.repository;

import java.util.List;
import net.checkconsulting.assuranceiard.api.model.AccesRefuse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccesRefuseRepository extends JpaRepository<AccesRefuse, Long> {

    List<AccesRefuse> findTop200ByOrderByDateTentativeDesc();
}
