package net.checkconsulting.assuranceiard.api.resource;

import net.checkconsulting.assuranceiard.api.dto.CompteResponse;
import net.checkconsulting.assuranceiard.api.dto.CreationCompteRequest;
import net.checkconsulting.assuranceiard.api.service.CompteService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CompteResource {

    private final CompteService compteService;

    @PostMapping("/comptes")
    public ResponseEntity<CompteResponse> creer(@Valid @RequestBody CreationCompteRequest demande) {
        CompteResponse compte = compteService.creer(demande);
        return ResponseEntity.created(URI.create("/comptes/" + compte.id())).body(compte);
    }
}
