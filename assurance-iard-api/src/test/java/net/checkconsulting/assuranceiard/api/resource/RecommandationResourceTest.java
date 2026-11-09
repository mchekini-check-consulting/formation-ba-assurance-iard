package net.checkconsulting.assuranceiard.api.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import net.checkconsulting.assuranceiard.api.service.RecommandationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(RecommandationResource.class)
class RecommandationResourceTest {

    private static final String VALIDE = """
            {"valeurContenu":15000,"objetsValeur":true,"appareilsCouteux":false,"remboursement":"vetuste",
            "assistance":true,"litiges":false,"priorite":"equilibre","formule":"confort",
            "raisons":["Vous déclarez des objets de valeur"],"options":["objets-valeur","assistance-depannage"]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommandationService recommandationService;

    private ResultActions poster(String corps) throws Exception {
        return mockMvc.perform(
                post("/devis/recommandations").contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    @Test
    void enregistreEtRenvoieLaReference() throws Exception {
        UUID reference = UUID.randomUUID();
        when(recommandationService.enregistrer(any())).thenReturn(reference);

        poster(VALIDE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value(reference.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "\"valeurContenu\":15000|\"valeurContenu\":-1",
        "\"valeurContenu\":15000|\"valeurContenu\":10000000",
        "\"objetsValeur\":true|\"objetsValeur\":null",
        "\"remboursement\":\"vetuste\"|\"remboursement\":\"autre\"",
        "\"priorite\":\"equilibre\"|\"priorite\":\"autre\"",
        "\"formule\":\"confort\"|\"formule\":\"autre\"",
        "[\"Vous déclarez des objets de valeur\"]|[]",
        "\"objets-valeur\",|\"autre\","
    })
    void refuseUneReponseInvalide(String remplacement) throws Exception {
        String[] parties = remplacement.split("\\|");

        poster(VALIDE.replace(parties[0], parties[1])).andExpect(status().isBadRequest());

        verifyNoInteractions(recommandationService);
    }
}
