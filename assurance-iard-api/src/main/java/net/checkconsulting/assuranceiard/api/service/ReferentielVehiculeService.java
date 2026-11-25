package net.checkconsulting.assuranceiard.api.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.MarqueResponse;
import net.checkconsulting.assuranceiard.api.dto.ModeleResponse;
import net.checkconsulting.assuranceiard.api.model.RefVehicule;
import net.checkconsulting.assuranceiard.api.repository.RefVehiculeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-27 : référentiel des véhicules groupés par marque pour le parcours de saisie manuelle. */
@Service
@RequiredArgsConstructor
public class ReferentielVehiculeService {

    private final RefVehiculeRepository vehicules;

    @Transactional(readOnly = true)
    public List<MarqueResponse> parMarque() {
        return vehicules.findAllByOrderByMarqueAscModeleAsc().stream()
                .collect(Collectors.groupingBy(RefVehicule::getMarque, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new MarqueResponse(e.getKey(), e.getValue().stream().map(this::versModele).toList()))
                .toList();
    }

    private ModeleResponse versModele(RefVehicule v) {
        return new ModeleResponse(v.getId(), v.getModele(), v.getEnergie(),
                v.getVersion(), v.getAnnee(), v.getValeurNeuve());
    }
}
