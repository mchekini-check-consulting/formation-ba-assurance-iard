package net.checkconsulting.assuranceiard.api.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import net.checkconsulting.assuranceiard.api.dto.ContexteDeclarationResponse;
import net.checkconsulting.assuranceiard.api.dto.SinistreEnvoyeResponse;
import net.checkconsulting.assuranceiard.api.dto.SinistreResumeResponse;
import net.checkconsulting.assuranceiard.api.dto.TypesSinistreResponse;
import net.checkconsulting.assuranceiard.api.exception.DeclarationDejaEnvoyeeException;
import net.checkconsulting.assuranceiard.api.exception.DeclarationImpossibleException;
import net.checkconsulting.assuranceiard.api.exception.DeclarationInvalideException;
import net.checkconsulting.assuranceiard.api.exception.GestionErreurs;
import net.checkconsulting.assuranceiard.api.exception.PieceRefuseeException;
import net.checkconsulting.assuranceiard.api.exception.SinistreInconnuException;
import net.checkconsulting.assuranceiard.api.exception.StockageIndisponibleException;
import net.checkconsulting.assuranceiard.api.service.sinistre.DeclarationSinistreService;
import net.checkconsulting.assuranceiard.api.service.sinistre.EnvoiDeclarationService;
import net.checkconsulting.assuranceiard.api.service.sinistre.PiecesJointesSinistreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** US-24 : routes de la déclaration et leurs codes HTTP (le contrôle d'accès est testé sur base réelle, SinistreIT). */
@WebMvcTest(SinistreResource.class)
@Import(GestionErreurs.class)
class SinistreResourceTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeclarationSinistreService declarations;

    @MockitoBean
    private EnvoiDeclarationService envois;

    @MockitoBean
    private PiecesJointesSinistreService pieces;

    @Test
    void ac1ContexteDeLaDeclaration() throws Exception {
        when(declarations.contexte(any())).thenReturn(new ContexteDeclarationResponse("Camille", "01 23 45 67 89",
                List.of()));

        mockMvc.perform(get("/sinistres/declaration")).andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroAssistance").value("01 23 45 67 89"));
    }

    @Test
    void ac1ContratNonDeclarable422() throws Exception {
        when(declarations.types("AE-RECETTE-A10")).thenThrow(new DeclarationImpossibleException());

        mockMvc.perform(get("/contrats/{numero}/types-sinistre", "AE-RECETTE-A10"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value(containsString("ne permet pas de déclarer")));
    }

    @Test
    void ac2TypesDuContrat() throws Exception {
        when(declarations.types("AE-RECETTE-A03")).thenReturn(new TypesSinistreResponse("AE-RECETTE-A03", "AUTO",
                "01", List.of(new TypesSinistreResponse.Type("AUTRE", "Autre / je ne sais pas", null, null, false,
                        List.of(), List.of()))));

        mockMvc.perform(get("/contrats/{numero}/types-sinistre", "AE-RECETTE-A03")).andExpect(status().isOk())
                .andExpect(jsonPath("$.types[0].code").value("AUTRE"));
    }

    @Test
    void ac8MesSinistres() throws Exception {
        when(declarations.mesSinistres(any())).thenReturn(List.of(new SinistreResumeResponse(1L, null, "BROUILLON",
                "VOL", "Vol du véhicule", "AE-RECETTE-A03", "AUTO", null, 2, false, null, Instant.now())));

        mockMvc.perform(get("/sinistres")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statut").value("BROUILLON"));
    }

    @Test
    void ac4ReponseInvalide400AvecUnMessageClair() throws Exception {
        when(declarations.mettreAJour(eq(42L), any())).thenThrow(new DeclarationInvalideException(
                "La date du sinistre ne peut pas être dans le futur."));

        mockMvc.perform(put("/sinistres/42").contentType(JSON).content("{\"type\":\"VOL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La date du sinistre ne peut pas être dans le futur."));
    }

    @Test
    void creationEtLectureDuBrouillon() throws Exception {
        mockMvc.perform(post("/sinistres").contentType(JSON)
                .content("{\"numeroContrat\":\"AE-RECETTE-A03\",\"type\":\"VOL\"}")).andExpect(status().isOk());
        when(declarations.lire(43L)).thenThrow(new SinistreInconnuException());
        mockMvc.perform(get("/sinistres/43")).andExpect(status().isNotFound());
    }

    @Test
    void ac9EnvoiEtDeclarationDejaEnvoyee409() throws Exception {
        when(envois.envoyer(eq(42L), any())).thenReturn(new SinistreEnvoyeResponse(42L, "SIN-2026-000042",
                "DECLARE", Instant.now(), true, true));
        mockMvc.perform(post("/sinistres/42/envoi").contentType(JSON).content("{\"declarationHonneur\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numero").value("SIN-2026-000042"));

        when(envois.envoyer(eq(41L), any())).thenThrow(new DeclarationDejaEnvoyeeException());
        mockMvc.perform(post("/sinistres/41/envoi").contentType(JSON).content("{\"declarationHonneur\":true}"))
                .andExpect(status().isConflict());
    }

    @Test
    void ac15EtAc16PieceRefusee422EtStockageIndisponible503() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "facture.exe", "application/octet-stream",
                new byte[] {'M', 'Z'});
        when(pieces.deposer(eq(42L), eq("PHOTOS"), any())).thenThrow(new PieceRefuseeException(
                PieceRefuseeException.FORMAT));
        mockMvc.perform(multipart("/sinistres/42/pieces").file(fichier).param("typePiece", "PHOTOS"))
                .andExpect(status().is(422)).andExpect(jsonPath("$.detail").value(PieceRefuseeException.FORMAT));

        when(pieces.deposer(eq(43L), eq("PHOTOS"), any())).thenThrow(new StockageIndisponibleException());
        mockMvc.perform(multipart("/sinistres/43/pieces").file(fichier).param("typePiece", "PHOTOS"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(containsString("envoyée sans cette pièce")));
    }

    @Test
    void telechargementEtSuppressionDUnePiece() throws Exception {
        when(pieces.lire(42L, 7L)).thenReturn(new PiecesJointesSinistreService.Fichier("photo dégâts.png", "PNG",
                new byte[] {1, 2}));

        mockMvc.perform(get("/sinistres/42/pieces/7")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Content-Disposition", containsString("inline")));
        mockMvc.perform(delete("/sinistres/42/pieces/7")).andExpect(status().isOk());
    }

    @Test
    void typeMediaSelonLeFormat() {
        assertThat(SinistreResource.typeMedia("pdf")).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(SinistreResource.typeMedia("JPG")).isEqualTo(MediaType.IMAGE_JPEG);
    }
}
