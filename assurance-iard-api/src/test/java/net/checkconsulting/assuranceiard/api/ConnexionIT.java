package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.service.EnvoiCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Parcours complets sur une vraie base. Le code envoyé par e-mail est intercepté (EnvoiCode simulé) ; le code de
 * test 123456 est éteint par défaut. Le bean simulé donne un contexte, donc une base, distinct de CompteIT.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ConnexionIT {

    private static final String MOT_DE_PASSE = "Abcdefg1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompteRepository compteRepository;

    @MockitoBean
    private EnvoiCode envoiCode;

    /** Dernier code envoyé par e-mail à cette adresse. */
    private String dernierCodeEnvoye(String email) {
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(envoiCode, atLeastOnce()).envoyer(eq(email), code.capture());
        return code.getValue();
    }

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
        String code = dernierCodeEnvoye("parcours@exemple.fr");
        assertThat(code).matches("\\d{6}");

        String session = saisirCode(verification, code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("parcours@exemple.fr"))
                .andReturn().getResponse().getContentAsString();
        String entete = "Bearer " + JsonPath.read(session, "$.jeton");

        mockMvc.perform(get("/session").header(HttpHeaders.AUTHORIZATION, entete))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("parcours@exemple.fr"));

        saisirCode(verification, code).andExpect(status().isNotFound());

        mockMvc.perform(delete("/session").header(HttpHeaders.AUTHORIZATION, entete)).andExpect(status().isNoContent());
        mockMvc.perform(get("/session").header(HttpHeaders.AUTHORIZATION, entete)).andExpect(status().isUnauthorized());
    }

    @Test
    void troisErreursDeCodeImposentUnNouveauCode() throws Exception {
        creerCompte("code@exemple.fr");
        String verification = demarrerVerification("code@exemple.fr");
        String premierCode = dernierCodeEnvoye("code@exemple.fr");
        String faux = autreCode(premierCode);

        saisirCode(verification, faux).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.essaisRestants").value(2));
        saisirCode(verification, faux).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.essaisRestants").value(1));
        saisirCode(verification, faux).andExpect(status().isConflict()).andExpect(jsonPath("$.motif").value("essais-epuises"));
        saisirCode(verification, premierCode).andExpect(status().isConflict());

        poster("/connexion/renvoi-code", "{\"verification\":\"" + verification + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailMasque").value("c***@exemple.fr"));
        String nouveauCode = dernierCodeEnvoye("code@exemple.fr");

        saisirCode(verification, nouveauCode).andExpect(status().isOk());
    }

    @Test
    void leRenvoiInvalideLeCodePrecedent() throws Exception {
        creerCompte("renvoi@exemple.fr");
        String verification = demarrerVerification("renvoi@exemple.fr");
        String premierCode = dernierCodeEnvoye("renvoi@exemple.fr");

        poster("/connexion/renvoi-code", "{\"verification\":\"" + verification + "\"}").andExpect(status().isOk());
        String nouveauCode = dernierCodeEnvoye("renvoi@exemple.fr");

        if (!premierCode.equals(nouveauCode)) {
            saisirCode(verification, premierCode).andExpect(status().isUnauthorized());
        }
        saisirCode(verification, nouveauCode).andExpect(status().isOk());
    }

    @Test
    void leCodeDeTestEstRefuseParDefaut() throws Exception {
        creerCompte("codetest@exemple.fr");
        String verification = demarrerVerification("codetest@exemple.fr");

        if (!"123456".equals(dernierCodeEnvoye("codetest@exemple.fr"))) {
            saisirCode(verification, "123456").andExpect(status().isUnauthorized());
        }
    }

    /** Un code à 6 chiffres forcément différent du vrai code. */
    private static String autreCode(String code) {
        return String.format("%06d", (Integer.parseInt(code) + 1) % 1_000_000);
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
