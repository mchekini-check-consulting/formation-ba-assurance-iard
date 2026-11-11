package net.checkconsulting.assuranceiard.api.repository;

import java.util.List;
import net.checkconsulting.assuranceiard.api.model.Permission;
import net.checkconsulting.assuranceiard.api.model.Profil;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    List<Permission> findByProfil(Profil profil);
}
