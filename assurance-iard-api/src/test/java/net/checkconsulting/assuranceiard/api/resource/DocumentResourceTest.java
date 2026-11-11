package net.checkconsulting.assuranceiard.api.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.DocumentResponse;
import net.checkconsulting.assuranceiard.api.exception.DocumentInconnuException;
import net.checkconsulting.assuranceiard.api.exception.GestionErreurs;
import net.checkconsulting.assuranceiard.api.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DocumentResource.class)
@Import(GestionErreurs.class)
class DocumentResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @Test
    void listeMesDocuments() throws Exception {
        when(documentService.lister("Bearer jeton")).thenReturn(List.of(new DocumentResponse(
                5L, UUID.randomUUID(), "identite", "passeport", "passeport.pdf", "pdf", 3)));

        mockMvc.perform(get("/documents").header(HttpHeaders.AUTHORIZATION, "Bearer jeton"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].nomFichier").value("passeport.pdf"));
    }

    @Test
    void ouvreUnDocument() throws Exception {
        when(documentService.ouvrir(5L)).thenReturn(new DocumentService.Fichier("pièce.pdf", "pdf", new byte[] {1}));

        mockMvc.perform(get("/documents/5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.startsWith("inline;")))
                .andExpect(content().bytes(new byte[] {1}));
    }

    @Test
    void unDocumentIntrouvableRepond404() throws Exception {
        when(documentService.ouvrir(9L)).thenThrow(new DocumentInconnuException());

        mockMvc.perform(get("/documents/9")).andExpect(status().isNotFound());
    }

    @Test
    void typeDuFichier() {
        assertThat(DocumentResource.typeMedia("pdf")).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(DocumentResource.typeMedia("png")).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(DocumentResource.typeMedia("jpg")).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(DocumentResource.typeMedia("autre")).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }
}
