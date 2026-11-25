package net.checkconsulting.moteurtarifaire.api.resource;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import net.checkconsulting.moteurtarifaire.api.dto.CriteresEligibiliteAutoResponse;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleIndisponibleException;
import net.checkconsulting.moteurtarifaire.api.service.EligibiliteAutoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

@WebMvcTest(EligibiliteAutoResource.class)
class EligibiliteAutoResourceTest {

    private static final String ADRESSE = "/v1/tarifs/auto/eligibilite";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EligibiliteAutoService service;

    @Test
    void lesCriteresDeGrilleActiveSontRenvoyes() throws Exception {
        when(service.criteres()).thenReturn(
                new CriteresEligibiliteAutoResponse(new BigDecimal("1.50"), "NON_PAIEMENT"));

        mockMvc.perform(get(ADRESSE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bonusMalusMaxLigne").value(1.5))
                .andExpect(jsonPath("$.resiliationBloquante").value("NON_PAIEMENT"));
    }

    @Test
    void sansGrilleLeServiceEstIndisponible() throws Exception {
        when(service.criteres()).thenThrow(new GrilleIndisponibleException(LocalDate.EPOCH));

        mockMvc.perform(get(ADRESSE))
                .andExpect(status().isServiceUnavailable());
    }
}
