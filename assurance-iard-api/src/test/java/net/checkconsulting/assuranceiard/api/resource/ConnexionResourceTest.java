package net.checkconsulting.assuranceiard.api.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import net.checkconsulting.assuranceiard.api.dto.SessionCouranteResponse;
import net.checkconsulting.assuranceiard.api.dto.SessionResponse;
import net.checkconsulting.assuranceiard.api.dto.VerificationResponse;
import net.checkconsulting.assuranceiard.api.exception.CodeIncorrectException;
import net.checkconsulting.assuranceiard.api.exception.CompteBloqueException;
import net.checkconsulting.assuranceiard.api.exception.IdentifiantsIncorrectsException;
import net.checkconsulting.assuranceiard.api.exception.NouveauCodeRequisException;
import net.checkconsulting.assuranceiard.api.exception.SessionInvalideException;
import net.checkconsulting.assuranceiard.api.exception.VerificationInconnueException;
import net.checkconsulting.assuranceiard.api.service.CodeVerificationService;
import net.checkconsulting.assuranceiard.api.service.ConnexionService;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(ConnexionResource.class)
class ConnexionResourceTest {

    private static final String CORPS_CONNEXION = "{\"email\":\"marie@exemple.fr\",\"motDePasse\":\"Abcdefg1\"}";
    private static final String CORPS_CODE = "{\"verification\":\"jeton\",\"code\":\"123456\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConnexionService connexionService;

    @MockitoBean
    private CodeVerificationService codeVerificationService;

    @MockitoBean
    private SessionService sessionService;

    private ResultActions poster(String chemin, String corps) throws Exception {
        return mockMvc.perform(post(chemin).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    @Test
    void connexionRenvoieLAdresseMasquee() throws Exception {
        when(connexionService.connecter(any())).thenReturn(new VerificationResponse("jeton", "m***@exemple.fr"));

        poster("/connexion", CORPS_CONNEXION)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verification").value("jeton"))
                .andExpect(jsonPath("$.emailMasque").value("m***@exemple.fr"));
    }

    @Test
    void connexionRenvoie401SansPreciserLeChampIncorrect() throws Exception {
        when(connexionService.connecter(any())).thenThrow(new IdentifiantsIncorrectsException());

        poster("/connexion", CORPS_CONNEXION)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou mot de passe incorrect."));
    }

    @Test
    void connexionRenvoie423AvecLaFinDuBlocage() throws Exception {
        when(connexionService.connecter(any()))
                .thenThrow(new CompteBloqueException(Instant.parse("2026-11-04T10:15:00Z")));

        poster("/connexion", CORPS_CONNEXION)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.detail").value("Compte bloqué 15 minutes."))
                .andExpect(jsonPath("$.bloqueJusquA").value("2026-11-04T10:15:00Z"));
    }

    @Test
    void connexionRefuseUnCorpsIncomplet() throws Exception {
        poster("/connexion", "{}").andExpect(status().isBadRequest());
        verifyNoInteractions(connexionService);
    }

    @Test
    void verificationOuvreLaSession() throws Exception {
        when(codeVerificationService.verifier("jeton", "123456"))
                .thenReturn(new SessionResponse("session", "marie@exemple.fr", "PROSPECT"));

        poster("/connexion/verification", CORPS_CODE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jeton").value("session"))
                .andExpect(jsonPath("$.email").value("marie@exemple.fr"));
    }

    @Test
    void verificationRenvoie401AvecLesEssaisRestants() throws Exception {
        when(codeVerificationService.verifier("jeton", "123456")).thenThrow(new CodeIncorrectException(2));

        poster("/connexion/verification", CORPS_CODE)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.essaisRestants").value(2));
    }

    @Test
    void verificationRenvoie409SiUnNouveauCodeEstNecessaire() throws Exception {
        when(codeVerificationService.verifier("jeton", "123456")).thenThrow(NouveauCodeRequisException.codeExpire());

        poster("/connexion/verification", CORPS_CODE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motif").value("code-expire"));
    }

    @Test
    void verificationRenvoie404SiElleNExistePlus() throws Exception {
        when(codeVerificationService.verifier("jeton", "123456")).thenThrow(new VerificationInconnueException());

        poster("/connexion/verification", CORPS_CODE).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567", "abcdef"})
    void verificationRefuseUnCodeQuiNAPasSixChiffres(String code) throws Exception {
        poster("/connexion/verification", "{\"verification\":\"jeton\",\"code\":\"" + code + "\"}")
                .andExpect(status().isBadRequest());
        verifyNoInteractions(codeVerificationService);
    }

    @Test
    void renvoiDuCode() throws Exception {
        when(codeVerificationService.renvoyer("jeton")).thenReturn(new VerificationResponse("jeton", "m***@exemple.fr"));

        poster("/connexion/renvoi-code", "{\"verification\":\"jeton\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailMasque").value("m***@exemple.fr"));
    }

    @Test
    void lectureDeLaSession() throws Exception {
        when(sessionService.lire("Bearer session")).thenReturn(new SessionCouranteResponse("marie@exemple.fr", "PROSPECT", List.of("DEVIS:CONSULTER")));

        mockMvc.perform(get("/session").header(HttpHeaders.AUTHORIZATION, "Bearer session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("marie@exemple.fr"))
                .andExpect(jsonPath("$.droits[0]").value("DEVIS:CONSULTER"));
    }

    @Test
    void lectureDeLaSessionRenvoie401SansSessionValide() throws Exception {
        when(sessionService.lire(null)).thenThrow(new SessionInvalideException());

        mockMvc.perform(get("/session")).andExpect(status().isUnauthorized());
    }

    @Test
    void deconnexion() throws Exception {
        mockMvc.perform(delete("/session").header(HttpHeaders.AUTHORIZATION, "Bearer session"))
                .andExpect(status().isNoContent());

        verify(sessionService).fermer("Bearer session");
    }
}
