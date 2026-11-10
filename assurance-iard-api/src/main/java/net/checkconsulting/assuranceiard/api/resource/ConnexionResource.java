package net.checkconsulting.assuranceiard.api.resource;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.dto.ConnexionRequest;
import net.checkconsulting.assuranceiard.api.dto.RenvoiCodeRequest;
import net.checkconsulting.assuranceiard.api.dto.SessionCouranteResponse;
import net.checkconsulting.assuranceiard.api.dto.SessionResponse;
import net.checkconsulting.assuranceiard.api.dto.VerificationCodeRequest;
import net.checkconsulting.assuranceiard.api.dto.VerificationResponse;
import net.checkconsulting.assuranceiard.api.service.CodeVerificationService;
import net.checkconsulting.assuranceiard.api.service.ConnexionService;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ConnexionResource {

    private final ConnexionService connexionService;
    private final CodeVerificationService codeVerificationService;
    private final SessionService sessionService;

    @PostMapping("/connexion")
    @LogMetier
    public VerificationResponse connecter(@Valid @RequestBody ConnexionRequest demande) {
        return connexionService.connecter(demande);
    }

    @PostMapping("/connexion/verification")
    @LogMetier
    public SessionResponse verifier(@Valid @RequestBody VerificationCodeRequest demande) {
        return codeVerificationService.verifier(demande.verification(), demande.code());
    }

    @PostMapping("/connexion/renvoi-code")
    public VerificationResponse renvoyerCode(@Valid @RequestBody RenvoiCodeRequest demande) {
        return codeVerificationService.renvoyer(demande.verification());
    }

    @GetMapping("/session")
    public SessionCouranteResponse lireSession(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return sessionService.lire(authorization);
    }

    @DeleteMapping("/session")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void fermerSession(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        sessionService.fermer(authorization);
    }
}
