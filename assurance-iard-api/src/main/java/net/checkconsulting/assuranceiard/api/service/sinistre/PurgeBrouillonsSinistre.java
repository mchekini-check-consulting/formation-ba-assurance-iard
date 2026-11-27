package net.checkconsulting.assuranceiard.api.service.sinistre;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.repository.PurgeBrouillonsRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-24, RG6 : un brouillon de sinistre est conservé 30 jours, puis supprimé avec ses pièces jointes. Passe toutes
 * les heures (premier passage 2 minutes après le démarrage) ; un dossier déclaré n'est jamais supprimé.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurgeBrouillonsSinistre {

    private final PurgeBrouillonsRepository purge;
    private final Clock horloge;

    @Scheduled(fixedDelayString = "${assurance.sinistres.purge.intervalle:PT1H}",
            initialDelayString = "${assurance.sinistres.purge.delai-initial:PT2M}")
    @Transactional
    public int purger() {
        int supprimes = purge.supprimerExpires(horloge.instant());
        if (supprimes > 0) {
            log.info("Brouillons de sinistre expirés supprimés : {}", supprimes);
        }
        return supprimes;
    }
}
