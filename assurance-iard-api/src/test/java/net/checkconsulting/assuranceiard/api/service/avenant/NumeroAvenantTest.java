package net.checkconsulting.assuranceiard.api.service.avenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import net.checkconsulting.assuranceiard.api.model.Avenant;
import org.junit.jupiter.api.Test;

/** US-23 : numéro AV-AAAA-NNNNNN, sur le modèle FAC-AAAA-NNNNNN. */
class NumeroAvenantTest {

    @Test
    void anneeDePreparationHeureDeParisEtIdentifiantSur6Chiffres() {
        Avenant avenant = new Avenant();
        avenant.setId(42L);
        // 31/12/2026 à 23h30 UTC = 01/01/2027 à Paris.
        avenant.setDateCreation(Instant.parse("2026-12-31T23:30:00Z"));

        assertThat(NumeroAvenant.de(avenant)).isEqualTo("AV-2027-000042");
        assertThat(NumeroAvenant.de(2026, 1_234_567)).isEqualTo("AV-2026-1234567");
    }

    @Test
    void retrouveLIdentifiantDUnNumero() {
        assertThat(NumeroAvenant.identifiant("AV-2026-000042")).contains(42L);
        assertThat(NumeroAvenant.identifiant("FAC-2026-000042")).isEmpty();
        assertThat(NumeroAvenant.identifiant(null)).isEmpty();
    }
}
