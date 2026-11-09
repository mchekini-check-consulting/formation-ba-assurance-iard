package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.cloud.openfeign.client.config.moteurtarifaire.read-timeout=" + MoteurTarifaireIT.DELAI_MAX_MS)
class MoteurTarifaireIT {

    static final int DELAI_MAX_MS = 300;
    private static final String ADRESSE = "/devis/tarifs";
    private static final String ADRESSE_MOTEUR = "/v1/tarifs/habitation";
    private static final String ADRESSE_POSTALE = "12 rue de la Paix";
    private static final String VILLE = "Lyon";
    private static final UUID ID_TARIF = UUID.randomUUID();
    private static final String TARIF = "{\"idTarif\":\"" + ID_TARIF + "\",\"versionGrille\":\"v\",\"tarifable\":true,"
            + "\"prix\":[{\"formule\":\"CONFORT\",\"mensuel\":11.99,\"annuel\":143.88}],\"champFuturDuMoteur\":1}";

    private static final AtomicReference<ReponseMoteur> REPONSE = new AtomicReference<>();
    private static final AtomicReference<String> CORPS_RECU = new AtomicReference<>();
    private static final AtomicReference<String> CHEMIN_RECU = new AtomicReference<>();
    private static final HttpServer FAUX_MOTEUR = demarrerFauxMoteur();

    private record ReponseMoteur(int statut, String corps, long attenteMs) {
    }

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void adresseDuMoteur(DynamicPropertyRegistry proprietes) {
        proprietes.add("moteur-tarifaire.url", () -> "http://localhost:" + FAUX_MOTEUR.getAddress().getPort());
    }

    @AfterAll
    static void arreter() {
        FAUX_MOTEUR.stop(0);
    }

    @BeforeEach
    void reinitialiser() {
        CORPS_RECU.set(null);
        CHEMIN_RECU.set(null);
    }

    private static String corps(String champ) {
        return """
                {"logement":{"statut":"locataire","residence":"principale","typeLogement":"appartement",
                "adresse":"%s","codePostal":"69003","ville":"%s","surface":"45",%s}}
                """.formatted(ADRESSE_POSTALE, VILLE, champ);
    }

    private ResultActions envoyer(String corps) throws Exception {
        return mockMvc.perform(post(ADRESSE).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    @Test
    void transmetLaDemandeEtRenvoieLeTarif() throws Exception {
        REPONSE.set(new ReponseMoteur(200, TARIF, 0));

        envoyer(corps("\"nombrePieces\":\"6\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTarif").value(ID_TARIF.toString()))
                .andExpect(jsonPath("$.prix[0].formule").value("confort"))
                .andExpect(jsonPath("$.prix[0].mensuel").value(11.99))
                .andExpect(jsonPath("$.champFuturDuMoteur").doesNotExist());

        assertThat(CHEMIN_RECU.get()).isEqualTo(ADRESSE_MOTEUR);
        assertThat(CORPS_RECU.get()).contains("\"statut\":\"LOCATAIRE\"", "\"pieces\":5", "\"typeLogement\":\"APPARTEMENT\"");
    }

    @Test
    void uneDonneePersonnelleNArriveJamaisAuMoteur() throws Exception {
        REPONSE.set(new ReponseMoteur(200, TARIF, 0));
        envoyer(corps("\"nombrePieces\":\"2\"")).andExpect(status().isOk());

        assertThat(CORPS_RECU.get()).doesNotContain(ADRESSE_POSTALE, VILLE);
    }

    @Test
    void transmetLesErreursDeChamp() throws Exception {
        REPONSE.set(new ReponseMoteur(400, "{\"erreurs\":[{\"champ\":\"etage\",\"message\":\"Obligatoire\"}]}", 0));

        envoyer(corps("\"nombrePieces\":\"2\"")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.erreurs[0].champ").value("etage"));
    }

    @Test
    void unMoteurEnErreurDonne503() throws Exception {
        REPONSE.set(new ReponseMoteur(500, "{}", 0));

        envoyer(corps("\"nombrePieces\":\"2\"")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void unMoteurTropLentDonne503() throws Exception {
        REPONSE.set(new ReponseMoteur(200, TARIF, DELAI_MAX_MS * 4L));

        envoyer(corps("\"nombrePieces\":\"2\"")).andExpect(status().isServiceUnavailable());
    }

    private static HttpServer demarrerFauxMoteur() {
        try {
            HttpServer serveur = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            serveur.createContext(ADRESSE_MOTEUR, MoteurTarifaireIT::repondre);
            serveur.setExecutor(Executors.newCachedThreadPool());
            serveur.start();
            return serveur;
        } catch (IOException erreur) {
            throw new IllegalStateException(erreur);
        }
    }

    private static void repondre(HttpExchange echange) throws IOException {
        CHEMIN_RECU.set(echange.getRequestURI().getPath());
        CORPS_RECU.set(new String(echange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        ReponseMoteur reponse = REPONSE.get();
        try {
            Thread.sleep(reponse.attenteMs());
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
        }
        byte[] octets = reponse.corps().getBytes(StandardCharsets.UTF_8);
        echange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        echange.sendResponseHeaders(reponse.statut(), octets.length);
        try (var sortie = echange.getResponseBody()) {
            sortie.write(octets);
        } catch (IOException clientParti) {
            echange.close();
        }
    }
}
