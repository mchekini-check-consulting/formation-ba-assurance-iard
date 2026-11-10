package net.checkconsulting.assuranceiard.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import net.checkconsulting.assuranceiard.api.model.LienReinitialisation;
import net.checkconsulting.assuranceiard.api.repository.LienReinitialisationRepository;
import net.checkconsulting.assuranceiard.api.service.EnvoiCode;
import net.checkconsulting.assuranceiard.api.service.EnvoiLienReinitialisation;
import net.checkconsulting.assuranceiard.api.utils.Jetons;
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

/** Parcours complets sur une vraie base. Le faux envoi donne un contexte, donc une base, distinct des autres IT. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
    "assurance.connexion.code-bouchon-actif=true",
    // US-06 : la connexion qui suit la réinitialisation utilise le code de test, sans envoi d'e-mail.
    "assurance.double-authentification.code-test-actif=true"})
@AutoConfigureMockMvc
class ReinitialisationIT {

    private static final String ANCIEN = "Abcdefg1";
    private static final String NOUVEAU = "Nouveau123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LienReinitialisationRepository lienRepository;

    @MockitoBean
    private EnvoiLienReinitialisation envoiLien;

    @MockitoBean
    private EnvoiCode envoiCode;

    private ResultActions poster(String chemin, String corps) throws Exception {
        return mockMvc.perform(post(chemin).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private static String identifiants(String email, String motDePasse) {
        return "{\"email\":\"" + email + "\",\"motDePasse\":\"" + motDePasse + "\"}";
    }

    private ResultActions connecter(String email, String motDePasse) throws Exception {
        return poster("/connexion", identifiants(email, motDePasse));
    }

    private void creerCompte(String email) throws Exception {
        poster("/comptes", identifiants(email, ANCIEN)).andExpect(status().isCreated());
    }

    private String demanderUnLien(String email) throws Exception {
        reset(envoiLien);
        poster("/mot-de-passe/oubli", "{\"email\":\"" + email + "\"}").andExpect(status().isNoContent());
        ArgumentCaptor<String> lien = ArgumentCaptor.forClass(String.class);
        verify(envoiLien).envoyer(eq(email), lien.capture());
        return lien.getValue().substring(lien.getValue().indexOf("jeton=") + "jeton=".length());
    }

    private ResultActions verifierLeLien(String jeton) throws Exception {
        return poster("/mot-de-passe/lien", "{\"jeton\":\"" + jeton + "\"}");
    }

    private ResultActions reinitialiser(String jeton, String motDePasse) throws Exception {
        return poster("/mot-de-passe/reinitialisation", "{\"jeton\":\"" + jeton + "\",\"motDePasse\":\"" + motDePasse + "\"}");
    }

    @Test
    void parcoursCompletPuisLienRefuseAuDeuxiemeUsage() throws Exception {
        creerCompte("parcours@exemple.fr");
        String jeton = demanderUnLien("parcours@exemple.fr");

        verifierLeLien(jeton).andExpect(status().isNoContent());
        reinitialiser(jeton, NOUVEAU).andExpect(status().isNoContent());

        connecter("parcours@exemple.fr", ANCIEN).andExpect(status().isUnauthorized());
        connecter("parcours@exemple.fr", NOUVEAU).andExpect(status().isOk());
        verifierLeLien(jeton).andExpect(status().isGone());
        reinitialiser(jeton, "Encore1234").andExpect(status().isGone());
    }

    @Test
    void repondPareilSansCompteEtNEnvoieRien() throws Exception {
        reset(envoiLien);

        poster("/mot-de-passe/oubli", "{\"email\":\"inconnu@exemple.fr\"}").andExpect(status().isNoContent());

        verify(envoiLien, never()).envoyer(anyString(), anyString());
    }

    @Test
    void unNouveauLienAnnuleLePrecedent() throws Exception {
        creerCompte("deux-liens@exemple.fr");
        String premier = demanderUnLien("deux-liens@exemple.fr");
        String second = demanderUnLien("deux-liens@exemple.fr");

        verifierLeLien(premier).andExpect(status().isGone());
        verifierLeLien(second).andExpect(status().isNoContent());
    }

    @Test
    void refuseUnLienExpire() throws Exception {
        creerCompte("expire@exemple.fr");
        String jeton = demanderUnLien("expire@exemple.fr");
        LienReinitialisation lien = lienRepository.findAll().stream()
                .filter(trouve -> trouve.getJetonHash().equals(Jetons.empreinte(jeton)))
                .findFirst().orElseThrow();
        lien.setDateExpiration(Instant.now().minusSeconds(1));
        lienRepository.save(lien);

        verifierLeLien(jeton).andExpect(status().isGone());
        reinitialiser(jeton, NOUVEAU).andExpect(status().isGone());
        connecter("expire@exemple.fr", ANCIEN).andExpect(status().isOk());
    }

    @Test
    void laReinitialisationLeveLeBlocage() throws Exception {
        creerCompte("bloque@exemple.fr");
        for (int essai = 0; essai < 5; essai++) {
            connecter("bloque@exemple.fr", "mauvais");
        }
        connecter("bloque@exemple.fr", ANCIEN).andExpect(status().isLocked());

        reinitialiser(demanderUnLien("bloque@exemple.fr"), NOUVEAU).andExpect(status().isNoContent());

        connecter("bloque@exemple.fr", NOUVEAU).andExpect(status().isOk());
    }

    @Test
    void laReinitialisationCoupeLesAccesEnCours() throws Exception {
        creerCompte("acces@exemple.fr");
        String demarrage = connecter("acces@exemple.fr", ANCIEN).andReturn().getResponse().getContentAsString();
        String verification = JsonPath.read(demarrage, "$.verification");
        String session = poster("/connexion/verification", "{\"verification\":\"" + verification + "\",\"code\":\"123456\"}")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String entete = "Bearer " + JsonPath.read(session, "$.jeton");
        String autreDemarrage = connecter("acces@exemple.fr", ANCIEN).andReturn().getResponse().getContentAsString();
        String autreVerification = JsonPath.read(autreDemarrage, "$.verification");

        reinitialiser(demanderUnLien("acces@exemple.fr"), NOUVEAU).andExpect(status().isNoContent());

        mockMvc.perform(get("/session").header(HttpHeaders.AUTHORIZATION, entete)).andExpect(status().isUnauthorized());
        poster("/connexion/verification", "{\"verification\":\"" + autreVerification + "\",\"code\":\"123456\"}")
                .andExpect(status().isNotFound());
    }
}
