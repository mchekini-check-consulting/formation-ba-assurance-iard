package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Parcours complets sur une vraie base. Le bouchon donne un contexte, donc une base, distinct de CompteIT. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "assurance.connexion.code-bouchon-actif=true")
@AutoConfigureMockMvc
class ConnexionIT {

    private static final String MOT_DE_PASSE = "Abcdefg1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompteRepository compteRepository;

    private ResultActions poster(String chemin, String corps) throws Exception {
        return mockMvc.perform(post(chemin).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private void creerCompte(String email) throws Exception {
        poster("/comptes", identifiants(email, MOT_DE_PASSE)).andExpect(status().isCreated());
    }

    private static String identifiants(String email, String motDePasse) {
        return "{\"email\":\"" + email + "\",\"motDePasse\":\"" + motDePasse + "\"}";
    }

    private ResultActions connecter(String email, String motDePasse) throws Exception {
        return poster("/connexion", identifiants(email, motDePasse));
    }

    private String demarrerVerification(String email) throws Exception {
        String reponse = connecter(email, MOT_DE_PASSE)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(reponse, "$.verification");
    }

    private ResultActions saisirCode(String verification, String code) throws Exception {
        return poster("/connexion/verification", "{\"verification\":\"" + verification + "\",\"code\":\"" + code + "\"}");
    }

    @Test
    void parcoursCompletJusquALaDeconnexion() throws Exception {
        creerCompte("parcours@exemple.fr");

        String reponse = connecter("Parcours@Exemple.fr", MOT_DE_PASSE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailMasque").value("p***@exemple.fr"))
                .andReturn().getResponse().getContentAsString();
        String verification = JsonPath.read(reponse, "$.verification");

        String session = saisirCode(verification, "123456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("parcours@exemple.fr"))
                .andReturn().getResponse().getContentAsString();
        String entete = "Bearer " + JsonPath.read(session, "$.jeton");

        mockMvc.perform(get("/session").header(HttpHeaders.AUTHORIZATION, entete))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("parcours@exemple.fr"));

        saisirCode(verification, "123456").andExpect(status().isNotFound());

        mockMvc.perform(delete("/session").header(HttpHeaders.AUTHORIZATION, entete)).andExpect(status().isNoContent());
        mockMvc.perform(get("/session").header(HttpHeaders.AUTHORIZATION, entete)).andExpect(status().isUnauthorized());
    }

    @Test
    void troisErreursDeCodeImposentUnNouveauCode() throws Exception {
        creerCompte("code@exemple.fr");
        String verification = demarrerVerification("code@exemple.fr");

        saisirCode(verification, "000000").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.essaisRestants").value(2));
        saisirCode(verification, "000000").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.essaisRestants").value(1));
        saisirCode(verification, "000000").andExpect(status().isConflict()).andExpect(jsonPath("$.motif").value("essais-epuises"));
        saisirCode(verification, "123456").andExpect(status().isConflict());

        poster("/connexion/renvoi-code", "{\"verification\":\"" + verification + "\"}").andExpect(status().isOk());

        saisirCode(verification, "123456").andExpect(status().isOk());
    }

    @Test
    void cinqEchecsDeMotDePasseBloquentLeCompteMemeAvecLeBonMotDePasse() throws Exception {
        creerCompte("blocage@exemple.fr");

        for (int essai = 0; essai < 4; essai++) {
            connecter("blocage@exemple.fr", "mauvais").andExpect(status().isUnauthorized());
        }
        connecter("blocage@exemple.fr", "mauvais")
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.bloqueJusquA").exists());
        connecter("blocage@exemple.fr", MOT_DE_PASSE).andExpect(status().isLocked());

        assertThat(compteRepository.existsByEmail("blocage@exemple.fr")).isTrue();
    }

    @Test
    void unSuccesRemetLeCompteurDEchecsAZero() throws Exception {
        creerCompte("consecutifs@exemple.fr");

        for (int essai = 0; essai < 4; essai++) {
            connecter("consecutifs@exemple.fr", "mauvais").andExpect(status().isUnauthorized());
        }
        demarrerVerification("consecutifs@exemple.fr");
        connecter("consecutifs@exemple.fr", "mauvais").andExpect(status().isUnauthorized());
    }

    @Test
    void unEmailInconnuRecoitLeMemeMessageQuUnMauvaisMotDePasse() throws Exception {
        connecter("inconnu@exemple.fr", MOT_DE_PASSE)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou mot de passe incorrect."));
    }
}
