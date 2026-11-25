package net.checkconsulting.assuranceiard.api.resource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import net.checkconsulting.assuranceiard.api.exception.DocumentAltereException;
import net.checkconsulting.assuranceiard.api.exception.DocumentIndisponibleException;
import net.checkconsulting.assuranceiard.api.exception.DocumentInconnuException;
import net.checkconsulting.assuranceiard.api.exception.GestionErreurs;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.service.DocumentService;
import net.checkconsulting.assuranceiard.api.service.document.DocumentsGeneresService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DocumentContratResource.class)
@Import(GestionErreurs.class)
class DocumentContratResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentsGeneresService service;

    @MockitoBean
    private DocumentRepository documents;

    @Test
    void telechargeLePdf() throws Exception {
        when(service.telecharger(5L)).thenReturn(new DocumentService.Fichier(
                "conditions-particulieres-AE-2026-000001-v2.pdf", "pdf", new byte[] {1, 2}));

        mockMvc.perform(get("/documents-contrat/5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.allOf(
                        Matchers.startsWith("attachment;"),
                        Matchers.containsString("conditions-particulieres-AE-2026-000001-v2.pdf"))))
                .andExpect(content().bytes(new byte[] {1, 2}));
    }

    @Test
    void ipidRetourneLePdfDeLaFormule() throws Exception {
        Document doc = mock(Document.class);
        when(doc.getId()).thenReturn(42L);
        when(documents.enVigueur(0L, "IPID", "INTER")).thenReturn(Optional.of(doc));
        when(service.telecharger(42L)).thenReturn(new DocumentService.Fichier("ipid-inter.pdf", "pdf", new byte[] {5}));

        mockMvc.perform(get("/documents-contrat/ipid/INTER"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(new byte[] {5}));
    }

    @Test
    void ipidReferenceInconnueEst404() throws Exception {
        mockMvc.perform(get("/documents-contrat/ipid/INCONNU"))
                .andExpect(status().isNotFound());
    }

    @Test
    void lesErreursOntUnCodeEtUnMessageClairs() throws Exception {
        when(service.telecharger(6L)).thenThrow(new DocumentInconnuException());
        when(service.telecharger(7L)).thenThrow(new DocumentIndisponibleException());
        when(service.telecharger(8L)).thenThrow(new DocumentAltereException());

        mockMvc.perform(get("/documents-contrat/6")).andExpect(status().isNotFound());
        mockMvc.perform(get("/documents-contrat/7")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("réessayez dans quelques minutes")));
        mockMvc.perform(get("/documents-contrat/8")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("contactez votre conseiller")));
    }
}
