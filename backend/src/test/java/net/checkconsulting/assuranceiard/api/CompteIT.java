package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CompteIT {

    private static final String CORPS = "{\"email\":\"Paul@Exemple.fr\",\"motDePasse\":\"Abcdefg1\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompteRepository compteRepository;

    @Test
    void enregistreLeCompteEnBaseEtRefuseUnDeuxiemeCompteAvecLeMemeEmail() throws Exception {
        mockMvc.perform(post("/comptes").contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isCreated());

        // La base est partagée avec les autres tests : seul le compte de ce test compte ici
        assertThat(compteRepository.findAll())
                .filteredOn(compte -> "paul@exemple.fr".equals(compte.getEmail()))
                .singleElement()
                .satisfies(compte -> {
                    assertThat(compte.getMotDePasseHash()).startsWith("$2").doesNotContain("Abcdefg1");
                    assertThat(compte.getDateCreation()).isNotNull();
                });

        mockMvc.perform(post("/comptes").contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isConflict());
    }
}
