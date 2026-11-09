package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** RG4 de l'US-10 sur une vraie base : migration, types des colonnes et relecture des listes. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RecommandationIT {

    private static final String CORPS = """
            {"valeurContenu":25000,"objetsValeur":true,"appareilsCouteux":false,"remboursement":"neuf",
            "assistance":true,"litiges":true,"priorite":"protection","formule":"serenite",
            "raisons":["Votre contenu dépasse 20 000 €","Vous souhaitez un remboursement à neuf"],
            "options":["objets-valeur","assistance-depannage","protection-juridique"]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecommandationDevisRepository repository;

    @Test
    void lesReponsesEtLaRecommandationSontRetrouveesEnBase() throws Exception {
        String reponse = mockMvc.perform(
                        post("/devis/recommandations").contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID reference = UUID.fromString(JsonPath.read(reponse, "$.reference"));

        RecommandationDevis enregistree = repository.findByReference(reference).orElseThrow();

        assertThat(enregistree.getValeurContenu()).isEqualTo(25_000);
        assertThat(enregistree.getFormule()).isEqualTo("serenite");
        assertThat(enregistree.getRaisons())
                .containsExactly("Votre contenu dépasse 20 000 €", "Vous souhaitez un remboursement à neuf");
        assertThat(enregistree.getOptions())
                .containsExactly("objets-valeur", "assistance-depannage", "protection-juridique");
        assertThat(enregistree.getDateCreation()).isNotNull();
    }
}
