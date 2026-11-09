package net.checkconsulting.assuranceiard.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Active les traitements automatiques, comme la suppression des devis trop anciens. */
@Configuration
@EnableScheduling
public class PlanificationConfig {
}
