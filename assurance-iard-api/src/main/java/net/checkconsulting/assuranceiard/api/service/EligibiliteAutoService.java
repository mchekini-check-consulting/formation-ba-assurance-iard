package net.checkconsulting.assuranceiard.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.client.MoteurTarifaireClient;
import net.checkconsulting.assuranceiard.api.dto.CriteresEligibiliteResponse;
import org.springframework.stereotype.Service;

/**
 * US-27 : critères d'éligibilité auto, chargés depuis le moteur tarifaire à la première demande
 * et mis en cache pour éviter un appel réseau à chaque requête.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EligibiliteAutoService {

    private final MoteurTarifaireClient client;
    private volatile CriteresEligibiliteResponse criteresCaches;

    public synchronized CriteresEligibiliteResponse criteres() {
        if (criteresCaches == null) {
            criteresCaches = client.criteresEligibiliteAuto();
            log.info("Critères d'éligibilité AUTO chargés depuis le moteur tarifaire");
        }
        return criteresCaches;
    }
}
