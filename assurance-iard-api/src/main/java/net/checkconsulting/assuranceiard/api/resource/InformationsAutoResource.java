package net.checkconsulting.assuranceiard.api.resource;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.InformationsAutoRequest;
import net.checkconsulting.assuranceiard.api.dto.InformationsAutoResponse;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.securite.Acces;
import net.checkconsulting.assuranceiard.api.service.InformationsAutoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** US-31 étape 4 : informations du souscripteur auto. */
@RestController
@RequestMapping("/v1/devis/auto")
@RequiredArgsConstructor
public class InformationsAutoResource {

    private final InformationsAutoService informationsAutoService;

    @GetMapping("/{id}/souscripteur")
    @Acces(rubrique = Rubrique.SOUSCRIPTION, droit = Droit.CONSULTER)
    public InformationsAutoResponse lire(
            @PathVariable Long id,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return informationsAutoService.lire(id, authorization);
    }

    @PostMapping("/{id}/souscripteur")
    @Acces(rubrique = Rubrique.SOUSCRIPTION, droit = Droit.CREER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enregistrer(
            @PathVariable Long id,
            @Valid @RequestBody InformationsAutoRequest demande,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        informationsAutoService.enregistrer(id, demande, authorization);
    }
}
