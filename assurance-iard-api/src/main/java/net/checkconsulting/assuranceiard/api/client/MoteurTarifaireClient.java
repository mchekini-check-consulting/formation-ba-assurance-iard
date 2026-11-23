package net.checkconsulting.assuranceiard.api.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.TarifAutoResponse;
import net.checkconsulting.assuranceiard.api.dto.TarifRequest;
import net.checkconsulting.assuranceiard.api.dto.TarifResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "moteurtarifaire",
        url = "${moteur-tarifaire.url}",
        configuration = MoteurTarifaireClientConfig.class)
public interface MoteurTarifaireClient {

    @PostMapping("/v1/tarifs/habitation")
    TarifResponse calculer(@RequestBody TarifRequest demande);

    @GetMapping("/v1/tarifs/habitation/{idTarif}")
    TarifResponse reprendre(@PathVariable("idTarif") UUID idTarif);

    @PostMapping("/v1/tarifs/auto")
    TarifAutoResponse calculerAuto(@RequestBody DemandeMoteurAuto demande);

    /** Corps envoyé à POST /v1/tarifs/auto du moteurtarifaire. */
    record DemandeMoteurAuto(
            Integer anneePermis,
            BigDecimal bonusMalus,
            Integer kmAnnuels,
            String usage,
            String stationnement,
            BigDecimal valeurVehicule,
            String formule,
            List<String> options) {
    }
}
