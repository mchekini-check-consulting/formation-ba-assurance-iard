package net.checkconsulting.assuranceiard.api.resource;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import net.checkconsulting.assuranceiard.api.dto.AccesRefuseResponse;
import net.checkconsulting.assuranceiard.api.dto.ClientResumeResponse;
import net.checkconsulting.assuranceiard.api.dto.DossierClientResponse;
import net.checkconsulting.assuranceiard.api.exception.ClientInconnuException;
import net.checkconsulting.assuranceiard.api.exception.GestionErreurs;
import net.checkconsulting.assuranceiard.api.securite.JournalAccesRefuses;
import net.checkconsulting.assuranceiard.api.service.BackOfficeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BackOfficeResource.class)
@Import(GestionErreurs.class)
class BackOfficeResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackOfficeService backOfficeService;

    @MockitoBean
    private JournalAccesRefuses journal;

    @Test
    void listeLesClients() throws Exception {
        when(backOfficeService.listerClients())
                .thenReturn(List.of(new ClientResumeResponse(1L, "Alice", "Martin", "alice@exemple.fr", "ASSURE")));

        mockMvc.perform(get("/back-office/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Martin"));
    }

    @Test
    void ouvreUnDossierClient() throws Exception {
        when(backOfficeService.dossier(1L)).thenReturn(new DossierClientResponse(1L, "madame", "Alice", "Martin",
                "alice@exemple.fr", "0612345678", "ASSURE", List.of(), List.of()));

        mockMvc.perform(get("/back-office/clients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@exemple.fr"));
    }

    @Test
    void unClientInconnuRepond404() throws Exception {
        when(backOfficeService.dossier(9L)).thenThrow(new ClientInconnuException());

        mockMvc.perform(get("/back-office/clients/9")).andExpect(status().isNotFound());
    }

    @Test
    void renvoieLeJournalDesAccesRefuses() throws Exception {
        when(journal.derniers()).thenReturn(List.of(new AccesRefuseResponse(Instant.parse("2026-11-11T10:00:00Z"),
                "alice@exemple.fr", "ASSURE", "GET", "/documents/9", "DOCUMENT", "9", "DOCUMENTS", "CONSULTER",
                "PAS_PROPRIETAIRE")));

        mockMvc.perform(get("/admin/acces-refuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].motif").value("PAS_PROPRIETAIRE"));
    }
}
