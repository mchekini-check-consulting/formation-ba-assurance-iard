package net.checkconsulting.assuranceiard.api.service.sinistre;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.checkconsulting.assuranceiard.api.repository.PurgeBrouillonsRepository;
import org.junit.jupiter.api.Test;

/** US-24, RG6 : la tâche planifiée supprime les brouillons expirés (le SQL est testé sur base réelle, SinistreIT). */
class PurgeBrouillonsSinistreTest {

    private final PurgeBrouillonsRepository purge = mock(PurgeBrouillonsRepository.class);
    private final PurgeBrouillonsSinistre tache = new PurgeBrouillonsSinistre(purge, FixturesSinistre.HORLOGE);

    @Test
    void rg6SupprimeLesBrouillonsExpiresALHeureDite() {
        when(purge.supprimerExpires(FixturesSinistre.MAINTENANT)).thenReturn(2);

        assertThat(tache.purger()).isEqualTo(2);
        verify(purge).supprimerExpires(FixturesSinistre.MAINTENANT);
    }

    @Test
    void rienASupprimer() {
        assertThat(tache.purger()).isZero();
    }

    @Test
    void rg5NumeroDeDossier() {
        assertThat(NumeroSinistre.de(FixturesSinistre.MAINTENANT, 42)).isEqualTo("SIN-2026-000042");
        // Le 31/12 à 23h30 UTC, il est déjà le 1er janvier à Paris.
        assertThat(NumeroSinistre.de(java.time.Instant.parse("2026-12-31T23:30:00Z"), 7))
                .isEqualTo("SIN-2027-000007");
    }
}
