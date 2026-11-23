package net.checkconsulting.assuranceiard.api.repository;

import java.util.List;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Permission;
import net.checkconsulting.assuranceiard.api.model.Profil;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    List<Permission> findByProfil(Profil profil);

    /** US-04, RG2 : vrai seulement si le droit existe et vaut « oui ». */
    boolean existsByProfilAndRubriqueAndDroitAndValeurTrue(Profil profil, Rubrique rubrique, Droit droit);
}
