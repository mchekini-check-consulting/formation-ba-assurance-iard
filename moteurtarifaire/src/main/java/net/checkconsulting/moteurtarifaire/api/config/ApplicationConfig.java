package net.checkconsulting.moteurtarifaire.api.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public Clock horloge() {
        return Clock.systemUTC();
    }
}
