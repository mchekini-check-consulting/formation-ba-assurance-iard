package net.checkconsulting.moteurtarifaire.api.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.moteurtarifaire.api.dto.TarifAutoRequest;
import net.checkconsulting.moteurtarifaire.api.dto.TarifAutoResponse;
import net.checkconsulting.moteurtarifaire.api.logging.LogMetier;
import net.checkconsulting.moteurtarifaire.api.service.TarificateurAuto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tarif auto")
@Slf4j
@RestController
@RequestMapping("/v1/tarifs/auto")
@RequiredArgsConstructor
public class TarifAutoResource {

    private final TarificateurAuto tarificateurAuto;

    @Operation(summary = "Calcule le tarif des 3 formules auto et, si demandé, le détail de la formule choisie")
    @ApiResponse(responseCode = "200", description = "Tarif calculé")
    @ApiResponse(responseCode = "400", description = "Champ absent ou invalide : voir la liste \"erreurs\"")
    @PostMapping
    @LogMetier(champs = {"demande.usage", "demande.stationnement"})
    public TarifAutoResponse calculer(@Valid @RequestBody TarifAutoRequest demande) {
        log.info("Demande de tarif AUTO reçue");
        return tarificateurAuto.calculer(demande);
    }
}
