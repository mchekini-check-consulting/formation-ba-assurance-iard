package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Les mêmes exemples figurent dans devis.service.spec.ts : l'écran et le backend doivent rester d'accord. */
class TarifServiceTest {

    private static final String TOUTES_OPTIONS = "objets-valeur+assistance-depannage+protection-juridique";

    private final TarifService service = new TarifService();

    @ParameterizedTest
    @CsvSource({
        "appartement, 30, essentielle, '', 300, 8.70",
        "appartement, 60, confort, assistance-depannage, 300, 13.72",
        "maison, 60, serenite, " + TOUTES_OPTIONS + ", 150, 23.92",
        "appartement, 9, essentielle, '', 500, 7.10",
        "maison, 1000, serenite, " + TOUTES_OPTIONS + ", 150, 69.04"
    })
    void calculeLeMemePrixQueLEcran(
            String type, int surface, String formule, String options, String franchise, String attendu) {
        List<String> liste = options.isEmpty() ? List.of() : Arrays.asList(options.split("\\+"));

        assertThat(service.prixMensuel(type, surface, formule, liste, franchise)).isEqualTo(attendu);
    }

    @Test
    void uneOptionEnDoubleNEstCompteeQuUneFois() {
        assertThat(service.prixMensuel("appartement", 60, "confort",
                List.of("assistance-depannage", "assistance-depannage"), "300")).isEqualTo("13.72");
    }

    @Test
    void lePrixAnnuelVautDouzeMois() {
        assertThat(service.prixAnnuel(new BigDecimal("13.72"))).isEqualTo("164.64");
    }
}
