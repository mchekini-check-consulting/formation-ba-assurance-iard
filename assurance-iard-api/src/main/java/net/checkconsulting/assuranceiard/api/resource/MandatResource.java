package net.checkconsulting.assuranceiard.api.resource;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.MandatPreparationRequest;
import net.checkconsulting.assuranceiard.api.dto.MandatPreparationResponse;
import net.checkconsulting.assuranceiard.api.dto.MandatSignatureRequest;
import net.checkconsulting.assuranceiard.api.dto.MandatSigneResponse;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.service.MandatService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MandatResource {

    private final MandatService mandatService;

    @PostMapping("/devis/mandats")
    @LogMetier
    public MandatPreparationResponse preparer(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody MandatPreparationRequest demande) {
        return mandatService.preparer(authorization, demande.referenceDevis());
    }

    @PostMapping("/devis/mandats/signature")
    @LogMetier
    public MandatSigneResponse signer(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody MandatSignatureRequest demande) {
        return mandatService.signer(authorization, demande);
    }
}
