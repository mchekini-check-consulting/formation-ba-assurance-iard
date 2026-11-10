package net.checkconsulting.assuranceiard.api.resource;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.ContratResponse;
import net.checkconsulting.assuranceiard.api.dto.EnvoyerCodeSignatureResponse;
import net.checkconsulting.assuranceiard.api.dto.GenerationContratRequest;
import net.checkconsulting.assuranceiard.api.dto.SignatureContratRequest;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.service.ContratService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ContratResource {

    private final ContratService contratService;

    @PostMapping("/devis/contrat")
    @LogMetier(champs = {"demande.typeLogement", "demande.surface", "demande.franchise"})
    public ContratResponse generer(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody GenerationContratRequest demande) {
        return contratService.generer(authorization, demande);
    }

    @GetMapping("/devis/contrat/{referenceDevis}")
    @LogMetier(champs = {"resultat.statut"})
    public ContratResponse obtenir(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID referenceDevis) {
        return contratService.obtenir(authorization, referenceDevis);
    }

    @PostMapping("/devis/contrat/{referenceDevis}/signature/code")
    @LogMetier
    public EnvoyerCodeSignatureResponse envoyerCode(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID referenceDevis) {
        return contratService.envoyerCode(authorization, referenceDevis);
    }

    @PostMapping("/devis/contrat/{referenceDevis}/signature/code/renvoyer")
    @LogMetier
    public EnvoyerCodeSignatureResponse renvoyerCode(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID referenceDevis) {
        return contratService.renvoyerCode(authorization, referenceDevis);
    }

    @PostMapping("/devis/contrat/{referenceDevis}/signature")
    @LogMetier(champs = {"resultat.statut"})
    public ContratResponse signer(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID referenceDevis,
            @Valid @RequestBody SignatureContratRequest demande) {
        return contratService.signer(authorization, referenceDevis, demande);
    }
}
