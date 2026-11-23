package net.checkconsulting.assuranceiard.api.resource;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.RecommandationRequest;
import net.checkconsulting.assuranceiard.api.dto.RecommandationResponse;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.service.RecommandationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import net.checkconsulting.assuranceiard.api.securite.AccesPublic;

@AccesPublic
@RestController
@RequiredArgsConstructor
public class RecommandationResource {

    private final RecommandationService recommandationService;

    @PostMapping("/devis/recommandations")
    @ResponseStatus(HttpStatus.CREATED)
    @LogMetier(champs = {"demande.formule", "demande.options", "resultat.reference"})
    public RecommandationResponse enregistrer(@Valid @RequestBody RecommandationRequest demande) {
        return new RecommandationResponse(recommandationService.enregistrer(demande));
    }
}
