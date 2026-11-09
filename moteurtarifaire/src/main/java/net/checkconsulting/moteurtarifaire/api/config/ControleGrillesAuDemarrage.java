package net.checkconsulting.moteurtarifaire.api.config;

import lombok.RequiredArgsConstructor;
import net.checkconsulting.moteurtarifaire.api.service.GrilleTarifaireService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ControleGrillesAuDemarrage implements ApplicationRunner {

    private final GrilleTarifaireService grilles;

    @Override
    public void run(ApplicationArguments arguments) {
        grilles.verifierToutes();
    }
}
