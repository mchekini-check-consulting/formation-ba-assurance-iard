package net.checkconsulting.assuranceiard.api.resource;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.DemandeTarif;
import net.checkconsulting.assuranceiard.api.dto.TarifResponse;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.service.TarifDevisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import net.checkconsulting.assuranceiard.api.securite.AccesPublic;

@AccesPublic
@RestController
@RequiredArgsConstructor
public class TarifResource {

    private final TarifDevisService tarifDevisService;

    @PostMapping("/devis/tarifs")
    @LogMetier(champs = {"demande.formule", "demande.franchise"})
    public TarifResponse estimer(@Valid @RequestBody DemandeTarif demande) {
        return tarifDevisService.estimer(demande);
    }

    @PostMapping("/devis/{reference}/tarif")
    @LogMetier(champs = {"demande.formule", "demande.franchise", "reference"})
    public TarifResponse tarifer(@PathVariable UUID reference, @Valid @RequestBody DemandeTarif demande) {
        return tarifDevisService.tarifer(reference, demande);
    }

    @GetMapping("/devis/{reference}/tarif")
    public TarifResponse tarifRetenu(@PathVariable UUID reference) {
        return tarifDevisService.tarifRetenu(reference);
    }
}
