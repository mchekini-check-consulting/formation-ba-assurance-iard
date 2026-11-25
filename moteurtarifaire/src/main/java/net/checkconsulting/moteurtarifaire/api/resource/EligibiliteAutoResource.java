package net.checkconsulting.moteurtarifaire.api.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.moteurtarifaire.api.dto.CriteresEligibiliteAutoResponse;
import net.checkconsulting.moteurtarifaire.api.service.EligibiliteAutoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US-27 : critères d'éligibilité à la souscription auto en ligne. */
@Tag(name = "Tarif auto")
@RestController
@RequestMapping("/v1/tarifs/auto/eligibilite")
@RequiredArgsConstructor
public class EligibiliteAutoResource {

    private final EligibiliteAutoService service;

    @Operation(summary = "Critères qui bloquent la souscription auto en ligne (bonus-malus max, résiliation bloquante)")
    @GetMapping
    public CriteresEligibiliteAutoResponse criteres() {
        return service.criteres();
    }
}
