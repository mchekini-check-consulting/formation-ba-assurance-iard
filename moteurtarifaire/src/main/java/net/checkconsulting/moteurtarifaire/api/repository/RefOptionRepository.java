package net.checkconsulting.moteurtarifaire.api.repository;

import java.util.List;
import net.checkconsulting.moteurtarifaire.api.model.RefOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefOptionRepository extends JpaRepository<RefOption, Long> {

    List<RefOption> findByGrilleIdAndProduitOrderByCode(Long grilleId, String produit);
}
