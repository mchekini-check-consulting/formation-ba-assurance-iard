package net.checkconsulting.assuranceiard.api.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

/**
 * Lecture des scripts SQL de données de test (dossier db/donnees-test, chargés seulement avec
 * DONNEES_TEST_ACTIVES=true). Commune aux classes DonneesRecette* qui les exécutent.
 */
public final class ScriptsDonneesTest {

    private ScriptsDonneesTest() {
    }

    /** Le contenu du script, en UTF-8 ; erreur explicite s'il est introuvable. */
    public static String lire(String chemin) {
        try {
            return new ClassPathResource(chemin).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Script de données de test introuvable : " + chemin, e);
        }
    }
}
