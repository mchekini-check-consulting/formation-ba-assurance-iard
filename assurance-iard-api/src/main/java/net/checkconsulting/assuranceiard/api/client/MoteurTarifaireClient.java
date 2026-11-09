package net.checkconsulting.assuranceiard.api.client;

import java.util.UUID;
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
}
