package net.checkconsulting.assuranceiard.api.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import net.checkconsulting.assuranceiard.api.repository.DevisEnregistreRepository;
import org.junit.jupiter.api.Test;

class ConservationDevisServiceTest {

    @Test
    void supprimeCeQuiDateDePlusDeTroisAns() {
        DevisEnregistreRepository repository = mock(DevisEnregistreRepository.class);
        Clock horloge = Clock.fixed(Instant.parse("2026-11-05T10:00:00Z"), ZoneOffset.UTC);

        new ConservationDevisService(repository, horloge).supprimerDevisTropAnciens();

        verify(repository).deleteByDateDernierContactBefore(Instant.parse("2023-11-05T10:00:00Z"));
    }
}
