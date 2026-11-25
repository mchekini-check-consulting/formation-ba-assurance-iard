package net.checkconsulting.moteurtarifaire.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.checkconsulting.moteurtarifaire.api.dto.CriteresEligibiliteAutoResponse;
import net.checkconsulting.moteurtarifaire.api.service.EligibiliteAutoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EligibiliteAutoIT {

    private static final String ADRESSE = "/v1/tarifs/auto/eligibilite";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EligibiliteAutoService service;

    @Test
    void lesCriteresDeGrilleAutoSontCharges() throws Exception {
        mockMvc.perform(get(ADRESSE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bonusMalusMaxLigne").value(1.5))
                .andExpect(jsonPath("$.resiliationBloquante").value("NON_PAIEMENT"));
    }

    @Test
    void leServiceRetourneLesValeursDeGrille() {
        CriteresEligibiliteAutoResponse criteres = service.criteres();

        assertThat(criteres.bonusMalusMaxLigne()).isEqualByComparingTo("1.5");
        assertThat(criteres.resiliationBloquante()).isEqualTo("NON_PAIEMENT");
    }
}
