package net.checkconsulting.moteurtarifaire.api.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.moteurtarifaire.api.dto.TarifHabitationRequest;
import net.checkconsulting.moteurtarifaire.api.dto.TarifHabitationResponse;
import net.checkconsulting.moteurtarifaire.api.service.TarifService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tarif habitation")
@Slf4j
@RestController
@RequestMapping("/v1/tarifs/habitation")
@RequiredArgsConstructor
public class TarifHabitationResource {

    private final TarifService tarifService;

    @Operation(summary = "Calcule le tarif des 3 formules et, si demandé, le détail et la formule conseillée")
    @ApiResponse(responseCode = "200", description = "Tarif calculé, ou orientation vers un conseiller (tarifable: false)")
    @ApiResponse(responseCode = "400", description = "Champ absent ou invalide : voir la liste \"erreurs\"")
    @PostMapping
    public TarifHabitationResponse calculer(@Valid @RequestBody TarifHabitationRequest demande) {
        log.info("Demande de tarif reçue");
        return tarifService.calculer(demande);
    }

    @Operation(summary = "Reprend un tarif à son prix d'origine, pendant sa durée de validité")
    @ApiResponse(responseCode = "200", description = "Le tarif tel qu'il a été calculé")
    @ApiResponse(responseCode = "404", description = "Identifiant inconnu")
    @ApiResponse(responseCode = "410", description = "Tarif expiré : un nouveau calcul est nécessaire")
    @GetMapping("/{idTarif}")
    public TarifHabitationResponse reprendre(@PathVariable UUID idTarif) {
        log.info("Demande de reprise d'un tarif : idTarif={}", idTarif);
        return tarifService.reprendre(idTarif);
    }
}
