package net.checkconsulting.assuranceiard.api.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "net.checkconsulting.assuranceiard.api.client")
public class FeignConfig {
}
