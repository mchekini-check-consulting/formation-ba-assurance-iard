package net.checkconsulting.moteurtarifaire.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Moteur tarifaire",
        version = "v1",
        description = "Calcule le tarif d'une assurance habitation et conseille une formule."))
public class OpenApiConfig {
}
