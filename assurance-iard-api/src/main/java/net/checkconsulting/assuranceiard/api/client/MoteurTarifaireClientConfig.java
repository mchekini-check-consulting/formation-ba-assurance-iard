package net.checkconsulting.assuranceiard.api.client;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

public class MoteurTarifaireClientConfig {

    @Bean
    public ErrorDecoder moteurTarifaireErreurs(JsonMapper jsonMapper) {
        return new MoteurTarifaireErreurDecoder(jsonMapper);
    }
}
