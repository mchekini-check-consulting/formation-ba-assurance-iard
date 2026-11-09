package net.checkconsulting.assuranceiard.api.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.checkconsulting.assuranceiard.api.dto.CompteResponse;
import net.checkconsulting.assuranceiard.api.exception.EmailDejaUtiliseException;
import net.checkconsulting.assuranceiard.api.service.CompteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CompteResource.class)
class CompteResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompteService compteService;

    private static String corps(String email, String motDePasse) {
        return "{\"email\":\"" + email + "\",\"motDePasse\":\"" + motDePasse + "\"}";
    }

    @Test
    void postComptesCreeLeCompte() throws Exception {
        when(compteService.creer(any())).thenReturn(new CompteResponse(7L, "marie@exemple.fr"));

        mockMvc.perform(post("/comptes").contentType(MediaType.APPLICATION_JSON).content(corps("marie@exemple.fr", "Abcdefg1")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/comptes/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("marie@exemple.fr"))
                .andExpect(jsonPath("$.motDePasse").doesNotExist());
    }

    @Test
    void postComptesRenvoie409SiEmailDejaUtilise() throws Exception {
        when(compteService.creer(any())).thenThrow(new EmailDejaUtiliseException());

        mockMvc.perform(post("/comptes").contentType(MediaType.APPLICATION_JSON).content(corps("deja@exemple.fr", "Abcdefg1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Un compte existe déjà avec cette adresse."));
    }

    @ParameterizedTest
    @CsvSource({
        "prenom.nom, Abcdefg1",
        "marie@exemple.fr, Abc1",
        "marie@exemple.fr, abcdefg1",
        "marie@exemple.fr, Abcdefgh",
    })
    void postComptesRefuseUneSaisieInvalide(String email, String motDePasse) throws Exception {
        mockMvc.perform(post("/comptes").contentType(MediaType.APPLICATION_JSON).content(corps(email, motDePasse)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(compteService);
    }
}
