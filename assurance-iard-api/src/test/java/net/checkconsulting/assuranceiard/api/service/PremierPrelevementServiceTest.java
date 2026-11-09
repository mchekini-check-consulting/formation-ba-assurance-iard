package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PremierPrelevementServiceTest {

    private final PremierPrelevementService service = new PremierPrelevementService();

    @ParameterizedTest(name = "début {0}, jour {1}, signature {2} : {3}")
    @CsvSource({
        // L'exemple du plan : le délai de 14 jours écarte le 10 novembre
        "2026-11-06, 10, 2026-11-05, 2026-12-10",
        // Le délai tombe exactement sur le jour choisi
        "2026-11-06, 15, 2026-11-01, 2026-11-15",
        // Un jour de moins que le délai : on passe au mois suivant
        "2026-11-06, 15, 2026-11-02, 2026-12-15",
        // La date de début, plus tardive que le délai, décide
        "2027-01-20, 5, 2026-11-05, 2027-02-05",
        "2027-01-05, 5, 2026-11-05, 2027-01-05",
        // Passage à l'année suivante
        "2026-12-20, 5, 2026-12-10, 2027-01-05",
        // Février court
        "2027-02-16, 15, 2026-12-01, 2027-03-15"
    })
    void calculeLePremierJourPossible(LocalDate debut, int jour, LocalDate signature, LocalDate attendu) {
        assertThat(service.calculer(debut, jour, signature)).isEqualTo(attendu);
    }
}
