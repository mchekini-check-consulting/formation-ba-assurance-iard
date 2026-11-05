package net.checkconsulting.assuranceiard.api.resource;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.IdentiteResponse;
import net.checkconsulting.assuranceiard.api.dto.InformationsRequest;
import net.checkconsulting.assuranceiard.api.service.InformationsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class InformationsResource {

    private final InformationsService informationsService;

    @GetMapping("/devis/informations")
    public IdentiteResponse lire(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return informationsService.lire(authorization);
    }

    @PostMapping(path = "/devis/informations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enregistrer(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestPart("informations") InformationsRequest demande,
            @RequestPart("pieceIdentite") MultipartFile pieceIdentite,
            @RequestPart("justificatifDomicile") MultipartFile justificatifDomicile) {
        informationsService.enregistrer(authorization, demande, pieceIdentite, justificatifDomicile);
    }
}
