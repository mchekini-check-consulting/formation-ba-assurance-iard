package net.checkconsulting.assuranceiard.api.resource;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import net.checkconsulting.assuranceiard.api.dto.TableauDeBordResponse;
import net.checkconsulting.assuranceiard.api.exception.GestionErreurs;
import net.checkconsulting.assuranceiard.api.exception.SessionInvalideException;
import net.checkconsulting.assuranceiard.api.service.EspaceClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EspaceClientResource.class)
@Import(GestionErreurs.class)
class EspaceClientResourceTest {

    private static final String JETON = "Bearer jeton";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EspaceClientService service;

    @Test
    void us18TableauDeBord() throws Exception {
        when(service.tableauDeBord(JETON)).thenReturn(new TableauDeBordResponse("David", List.of(), true, List.of(),
                List.of(), false, List.of()));

        mockMvc.perform(get("/espace-client/tableau-de-bord").header(HttpHeaders.AUTHORIZATION, JETON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prenom").value("David"))
                .andExpect(jsonPath("$.contrats").isEmpty())
                .andExpect(jsonPath("$.sinistresVisibles").value(false));
    }

    @Test
    void sansSessionValableLeTableauDeBordEstRefuse() throws Exception {
        when(service.tableauDeBord(null)).thenThrow(new SessionInvalideException());

        mockMvc.perform(get("/espace-client/tableau-de-bord"))
                .andExpect(status().isUnauthorized());
    }
}
