package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;
import net.checkconsulting.assuranceiard.api.client.MoteurTarifaireClient;
import net.checkconsulting.assuranceiard.api.dto.TarifRequest;
import net.checkconsulting.assuranceiard.api.dto.TarifResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MoteurTarifaireServiceTest {

    private static final UUID ID_TARIF = UUID.randomUUID();
    private static final TarifResponse TARIF =
            new TarifResponse(ID_TARIF, "version", true, null, null, null, null, null, null, null, null, null, null);

    @Mock
    private MoteurTarifaireClient client;

    @InjectMocks
    private MoteurTarifaireService service;

    @Test
    void renvoieLeTarifDuMoteur() {
        TarifRequest demande =
                new TarifRequest(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        when(client.calculer(demande)).thenReturn(TARIF);

        assertThat(service.calculer(demande)).isSameAs(TARIF);
    }

    @Test
    void reprendUnTarifAupresDuMoteur() {
        when(client.reprendre(ID_TARIF)).thenReturn(TARIF);

        assertThat(service.reprendre(ID_TARIF)).isSameAs(TARIF);
    }
}
