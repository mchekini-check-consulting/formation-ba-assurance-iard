package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import net.checkconsulting.assuranceiard.api.client.MoteurTarifaireClient;
import net.checkconsulting.assuranceiard.api.dto.CriteresEligibiliteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EligibiliteAutoServiceTest {

    @Mock
    private MoteurTarifaireClient client;

    private EligibiliteAutoService service;

    @BeforeEach
    void setUp() {
        service = new EligibiliteAutoService(client);
    }

    @Test
    void lesCriteresChargésDuMoteurSontRetournes() {
        CriteresEligibiliteResponse criteres = new CriteresEligibiliteResponse(new BigDecimal("1.50"), "NON_PAIEMENT");
        when(client.criteresEligibiliteAuto()).thenReturn(criteres);

        CriteresEligibiliteResponse resultat = service.criteres();

        assertThat(resultat.bonusMalusMaxLigne()).isEqualByComparingTo("1.50");
        assertThat(resultat.resiliationBloquante()).isEqualTo("NON_PAIEMENT");
    }

    @Test
    void leMoteurNEstAppeleQuUneFois() {
        CriteresEligibiliteResponse criteres = new CriteresEligibiliteResponse(new BigDecimal("1.50"), "NON_PAIEMENT");
        when(client.criteresEligibiliteAuto()).thenReturn(criteres);

        service.criteres();
        service.criteres();
        service.criteres();

        verify(client, times(1)).criteresEligibiliteAuto();
    }
}
