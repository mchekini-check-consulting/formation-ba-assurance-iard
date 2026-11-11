package net.checkconsulting.assuranceiard.api.resource;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.LienReinitialisationRequest;
import net.checkconsulting.assuranceiard.api.dto.MotDePasseOublieRequest;
import net.checkconsulting.assuranceiard.api.dto.NouveauMotDePasseRequest;
import net.checkconsulting.assuranceiard.api.service.ReinitialisationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import net.checkconsulting.assuranceiard.api.securite.AccesPublic;

@AccesPublic
@RestController
@RequiredArgsConstructor
public class MotDePasseResource {

    private final ReinitialisationService reinitialisationService;

    @PostMapping("/mot-de-passe/oubli")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void demanderUnLien(@Valid @RequestBody MotDePasseOublieRequest demande) {
        reinitialisationService.demander(demande.email());
    }

    @PostMapping("/mot-de-passe/lien")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifierLeLien(@Valid @RequestBody LienReinitialisationRequest demande) {
        reinitialisationService.verifier(demande.jeton());
    }

    @PostMapping("/mot-de-passe/reinitialisation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reinitialiser(@Valid @RequestBody NouveauMotDePasseRequest demande) {
        reinitialisationService.reinitialiser(demande.jeton(), demande.motDePasse());
    }
}
