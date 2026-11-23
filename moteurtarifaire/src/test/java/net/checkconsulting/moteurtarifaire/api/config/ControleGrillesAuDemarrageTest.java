package net.checkconsulting.moteurtarifaire.api.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import net.checkconsulting.moteurtarifaire.api.exception.GrilleInvalideException;
import net.checkconsulting.moteurtarifaire.api.service.GrilleTarifaireAutoService;
import net.checkconsulting.moteurtarifaire.api.service.GrilleTarifaireService;
import org.junit.jupiter.api.Test;

class ControleGrillesAuDemarrageTest {

    private final GrilleTarifaireService grilles = mock(GrilleTarifaireService.class);
    private final GrilleTarifaireAutoService grillesAuto = mock(GrilleTarifaireAutoService.class);
    private final ControleGrillesAuDemarrage controle = new ControleGrillesAuDemarrage(grilles, grillesAuto);

    @Test
    void controleLesGrillesHabitationAuDemarrage() {
        controle.run(null);

        verify(grilles).verifierToutes();
    }

    @Test
    void controleLesGrillesAutoAuDemarrage() {
        controle.run(null);

        verify(grillesAuto).verifier();
    }

    @Test
    void uneGrilleHabitationInvalideInterromptLeDemarrage() {
        doThrow(new GrilleInvalideException("invalide")).when(grilles).verifierToutes();

        assertThatThrownBy(() -> controle.run(null)).isInstanceOf(GrilleInvalideException.class);
    }

    @Test
    void uneGrilleAutoInvalideInterromptLeDemarrage() {
        doThrow(new GrilleInvalideException("invalide auto")).when(grillesAuto).verifier();

        assertThatThrownBy(() -> controle.run(null)).isInstanceOf(GrilleInvalideException.class);
    }
}
