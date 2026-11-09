package net.checkconsulting.assuranceiard.api.resource;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.checkconsulting.assuranceiard.api.exception.LienInvalideException;
import net.checkconsulting.assuranceiard.api.service.ReinitialisationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(MotDePasseResource.class)
class MotDePasseResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReinitialisationService reinitialisationService;

    private ResultActions poster(String chemin, String corps) throws Exception {
        return mockMvc.perform(post(chemin).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    @Test
    void demandeDeLien() throws Exception {
        poster("/mot-de-passe/oubli", "{\"email\":\"marie@exemple.fr\"}").andExpect(status().isNoContent());

        verify(reinitialisationService).demander("marie@exemple.fr");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"email\":\"\"}", "{\"email\":\"prenom.nom\"}"})
    void demandeDeLienRefuseUneAdresseInvalide(String corps) throws Exception {
        poster("/mot-de-passe/oubli", corps).andExpect(status().isBadRequest());

        verifyNoInteractions(reinitialisationService);
    }

    @Test
    void verificationDuLien() throws Exception {
        poster("/mot-de-passe/lien", "{\"jeton\":\"jeton\"}").andExpect(status().isNoContent());

        verify(reinitialisationService).verifier("jeton");
    }

    @Test
    void lienInvalideRenvoie410() throws Exception {
        doThrow(new LienInvalideException()).when(reinitialisationService).verifier("jeton");

        poster("/mot-de-passe/lien", "{\"jeton\":\"jeton\"}")
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.detail").value("Ce lien n'est plus valable."));
    }

    @Test
    void reinitialisation() throws Exception {
        poster("/mot-de-passe/reinitialisation", "{\"jeton\":\"jeton\",\"motDePasse\":\"Nouveau123\"}")
                .andExpect(status().isNoContent());

        verify(reinitialisationService).reinitialiser("jeton", "Nouveau123");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Abc1", "abcdefg1", "Abcdefgh"})
    void reinitialisationAppliqueLesReglesDeLUs05(String motDePasse) throws Exception {
        poster("/mot-de-passe/reinitialisation", "{\"jeton\":\"jeton\",\"motDePasse\":\"" + motDePasse + "\"}")
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reinitialisationService);
    }
}
