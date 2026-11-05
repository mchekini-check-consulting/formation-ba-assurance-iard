package net.checkconsulting.assuranceiard.api.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.repository.DevisEnregistreRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RGPD (limitation de la conservation) : un devis est supprimé 3 ans après le dernier contact du client. */
@Service
@RequiredArgsConstructor
public class ConservationDevisService {

    static final int DUREE_CONSERVATION_ANS = 3;
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");

    private final DevisEnregistreRepository repository;
    private final Clock horloge;

    @Scheduled(cron = "0 30 3 * * *", zone = "Europe/Paris")
    @Transactional
    public void supprimerDevisTropAnciens() {
        Instant limite = horloge.instant().atZone(FRANCE).minusYears(DUREE_CONSERVATION_ANS).toInstant();
        repository.deleteByDateDernierContactBefore(limite);
    }
}
