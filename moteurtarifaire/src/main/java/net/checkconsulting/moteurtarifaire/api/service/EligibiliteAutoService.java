package net.checkconsulting.moteurtarifaire.api.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.moteurtarifaire.api.dto.CriteresEligibiliteAutoResponse;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleIndisponibleException;
import net.checkconsulting.moteurtarifaire.api.model.ParametreTarifaire;
import net.checkconsulting.moteurtarifaire.api.model.VersionGrille;
import net.checkconsulting.moteurtarifaire.api.repository.ParametreTarifaireRepository;
import net.checkconsulting.moteurtarifaire.api.repository.VersionGrilleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-27 : fournit les critères qui bloquent la souscription auto en ligne. */
@Slf4j
@Service
@RequiredArgsConstructor
public class EligibiliteAutoService {

    private static final String AUTO = "AUTO";
    private static final String CATEGORIE = "ELIGIBILITE";

    private final VersionGrilleRepository versions;
    private final ParametreTarifaireRepository parametres;
    private final Clock horloge;

    @Transactional(readOnly = true)
    public CriteresEligibiliteAutoResponse criteres() {
        LocalDate aujourd = LocalDate.now(horloge);
        List<VersionGrille> applicables = versions.findApplicablesAu(aujourd, AUTO);
        if (applicables.isEmpty()) {
            throw new GrilleIndisponibleException(aujourd);
        }
        VersionGrille grille = applicables.getFirst();
        List<ParametreTarifaire> lignes = parametres.findByGrilleIdAndCategorie(grille.getId(), CATEGORIE);
        Map<String, String> map = lignes.stream()
                .collect(Collectors.toMap(ParametreTarifaire::getCle, ParametreTarifaire::getValeur));
        log.info("Critères d'éligibilité AUTO chargés depuis la grille {}", grille.getVersion());
        return new CriteresEligibiliteAutoResponse(
                new BigDecimal(map.get("BONUS_MALUS_MAX_LIGNE")),
                map.get("RESILIATION_BLOQUANTE"));
    }
}
