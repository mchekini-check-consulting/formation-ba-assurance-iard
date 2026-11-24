package net.checkconsulting.assuranceiard.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EcheanceAnnuelleTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 24);

    @Test
    void contratDeMoinsDUnAnEcheanceAuPremierAnniversaire() {
        assertThat(EcheanceAnnuelle.prochaine(LocalDate.of(2026, 9, 25), null, AUJOURDHUI))
                .isEqualTo(LocalDate.of(2027, 9, 25));
    }

    @Test
    void contratQuiNAPasEncorePrisEffetEcheanceUnAnApresLEffet() {
        assertThat(EcheanceAnnuelle.prochaine(LocalDate.of(2026, 12, 9), null, AUJOURDHUI))
                .isEqualTo(LocalDate.of(2027, 12, 9));
    }

    @Test
    void contratAncienEcheanceAuProchainAnniversaire() {
        assertThat(EcheanceAnnuelle.prochaine(LocalDate.of(2020, 3, 1), null, AUJOURDHUI))
                .isEqualTo(LocalDate.of(2027, 3, 1));
        assertThat(EcheanceAnnuelle.prochaine(LocalDate.of(2020, 12, 31), null, AUJOURDHUI))
                .isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void lAnniversaireDuJourEstLEcheance() {
        assertThat(EcheanceAnnuelle.prochaine(LocalDate.of(2024, 11, 24), null, AUJOURDHUI)).isEqualTo(AUJOURDHUI);
    }

    @Test
    void unEffetAu29FevrierTombeLe28LesAnneesNonBissextiles() {
        assertThat(EcheanceAnnuelle.prochaine(LocalDate.of(2024, 2, 29), null, AUJOURDHUI))
                .isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(EcheanceAnnuelle.prochaine(LocalDate.of(2024, 2, 29), null, LocalDate.of(2027, 3, 1)))
                .isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void unContratQuiFinitAvantLEcheanceNEnAPas() {
        assertThat(EcheanceAnnuelle.prochaine(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 12, 24), AUJOURDHUI))
                .isNull();
        assertThat(EcheanceAnnuelle.prochaine(LocalDate.of(2026, 9, 25), LocalDate.of(2027, 9, 25), AUJOURDHUI))
                .isEqualTo(LocalDate.of(2027, 9, 25));
    }

    @Test
    void sansDateDEffetPasDEcheance() {
        assertThat(EcheanceAnnuelle.prochaine(null, null, AUJOURDHUI)).isNull();
    }
}
