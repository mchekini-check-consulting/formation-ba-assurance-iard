package net.checkconsulting.assuranceiard.api.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-27 : plaques de test ERREUR et DELAI pour le simulateur SIV, en complément des données T-04.
 * Même interrupteur ({@code DONNEES_TEST_ACTIVES=true}). Le script est idempotent.
 */
@Slf4j
@Component
@Order(DonneesRecette.ORDRE + 3)
@ConditionalOnProperty(name = "assurance.donnees-test.actif", havingValue = "true")
@RequiredArgsConstructor
public class DonneesRecetteSprint4 implements ApplicationRunner {

    static final String SCRIPT = "db/donnees-test/donnees_recette_us27.sql";

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        String script;
        try {
            script = new ClassPathResource(SCRIPT).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Script de données de test introuvable : " + SCRIPT, e);
        }
        jdbc.execute(script);
        log.info("Données de recette US-27 vérifiées");
    }
}
