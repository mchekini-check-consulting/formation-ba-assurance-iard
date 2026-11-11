package net.checkconsulting.assuranceiard.api.repository;

import java.util.List;
import net.checkconsulting.assuranceiard.api.model.HistoriquePermission;
import net.checkconsulting.assuranceiard.api.model.Profil;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoriquePermissionRepository extends JpaRepository<HistoriquePermission, Long> {

    List<HistoriquePermission> findByProfilOrderByModifieLeDesc(Profil profil);
}
