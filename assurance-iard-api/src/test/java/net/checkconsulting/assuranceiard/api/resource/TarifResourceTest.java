package net.checkconsulting.assuranceiard.api.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.ErreurChampMoteur;
import net.checkconsulting.assuranceiard.api.dto.TarifResponse;
import net.checkconsulting.assuranceiard.api.exception.MoteurDonneesInvalidesException;
import net.checkconsulting.assuranceiard.api.exception.MoteurIndisponibleException;
import net.checkconsulting.assuranceiard.api.exception.MoteurTarifExpireException;
import net.checkconsulting.assuranceiard.api.exception.MoteurTarifInconnuException;
import net.checkconsulting.assuranceiard.api.exception.TarifDevisIncoherentException;
import net.checkconsulting.assuranceiard.api.service.TarifDevisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(TarifResource.class)
class TarifResourceTest {

    private static final String ADRESSE = "/devis/tarifs";
    private static final UUID ID_TARIF = UUID.randomUUID();
    private static final UUID REFERENCE = UUID.randomUUID();
    private static final TarifResponse TARIF =
            new TarifResponse(ID_TARIF, "version", true, null, null, null, null, null, null, null, null, null, null);
    private static final String CORPS = """
            {"logement":{"typeLogement":"appartement","adresse":"12 rue","codePostal":"69003","ville":"Lyon","surface":"45"}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TarifDevisService service;

    private ResultActions envoyer(String corps) throws Exception {
        return mockMvc.perform(post(ADRESSE).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    @Test
    void renvoieLeTarif() throws Exception {
        when(service.estimer(any())).thenReturn(TARIF);

        envoyer(CORPS).andExpect(status().isOk())
                .andExpect(jsonPath("$.idTarif").value(ID_TARIF.toString()))
                .andExpect(jsonPath("$.motif").doesNotExist());
    }

    @Test
    void refuseUneDemandeSansLogement() throws Exception {
        envoyer("{}").andExpect(status().isBadRequest());
    }

    @Test
    void transmetLesErreursDeChampDuMoteur() throws Exception {
        when(service.estimer(any())).thenThrow(
                new MoteurDonneesInvalidesException(List.of(new ErreurChampMoteur("etage", "Obligatoire"))));

        envoyer(CORPS).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs[0].champ").value("etage"))
                .andExpect(jsonPath("$.erreurs[0].message").value("Obligatoire"));
    }

    @Test
    void moteurIndisponibleDonne503() throws Exception {
        when(service.estimer(any())).thenThrow(new MoteurIndisponibleException());

        envoyer(CORPS).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(MoteurIndisponibleException.MESSAGE));
    }

    @Test
    void retientUnTarifPourUnDevis() throws Exception {
        when(service.tarifer(any(), any())).thenReturn(TARIF);

        mockMvc.perform(post("/devis/" + REFERENCE + "/tarif").contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTarif").value(ID_TARIF.toString()));
    }

    @Test
    void relitLeTarifRetenu() throws Exception {
        when(service.tarifRetenu(REFERENCE)).thenReturn(TARIF);

        mockMvc.perform(get("/devis/" + REFERENCE + "/tarif")).andExpect(status().isOk());
    }

    @Test
    void sansTarifRetenuLaRelectureDonne404() throws Exception {
        when(service.tarifRetenu(REFERENCE)).thenThrow(new MoteurTarifInconnuException());

        mockMvc.perform(get("/devis/" + REFERENCE + "/tarif")).andExpect(status().isNotFound());
    }

    @Test
    void unTarifExpireDonne410() throws Exception {
        when(service.tarifRetenu(REFERENCE)).thenThrow(new MoteurTarifExpireException());

        mockMvc.perform(get("/devis/" + REFERENCE + "/tarif")).andExpect(status().isGone());
    }

    @Test
    void unTarifIncoherentDonne409() throws Exception {
        when(service.tarifer(any(), any())).thenThrow(new TarifDevisIncoherentException());

        mockMvc.perform(post("/devis/" + REFERENCE + "/tarif").contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isConflict());
    }
}
