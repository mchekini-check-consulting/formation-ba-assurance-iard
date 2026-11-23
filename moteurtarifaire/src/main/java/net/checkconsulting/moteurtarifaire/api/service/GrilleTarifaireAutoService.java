package net.checkconsulting.moteurtarifaire.api.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaireAuto;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleInvalideException;
import net.checkconsulting.moteurtarifaire.api.mapper.GrilleTarifaireAutoAssembleur;
import net.checkconsulting.moteurtarifaire.api.model.VersionGrille;
import net.checkconsulting.moteurtarifaire.api.repository.RefFormuleRepository;
import net.checkconsulting.moteurtarifaire.api.repository.RefOptionRepository;
import net.checkconsulting.moteurtarifaire.api.repository.RefTarifRepository;
import net.checkconsulting.moteurtarifaire.api.repository.VersionGrilleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrilleTarifaireAutoService {

    private static final String AUTO = "AUTO";

    private final VersionGrilleRepository versions;
    private final RefFormuleRepository formules;
    private final RefOptionRepository options;
    private final RefTarifRepository tarifs;
    private final GrilleTarifaireAutoAssembleur assembleur;

    @Transactional(readOnly = true)
    public Optional<GrilleTarifaireAuto> applicableAu(LocalDate date) {
        List<VersionGrille> applicables = versions.findApplicablesAu(date, AUTO);
        if (applicables.isEmpty()) {
            log.warn("Aucune grille tarifaire AUTO en vigueur au {}", date);
        }
        if (applicables.size() > 1) {
            log.error("Plusieurs grilles tarifaires AUTO en vigueur au {}", date);
            throw new GrilleInvalideException("Plusieurs grilles tarifaires AUTO sont en vigueur au " + date + ".");
        }
        return applicables.stream().findFirst().map(this::charger);
    }

    @Transactional(readOnly = true)
    public void verifier() {
        List<VersionGrille> toutes = versions.findByProduitOrderByDateDebutAsc(AUTO);
        if (toutes.isEmpty()) {
            throw new GrilleInvalideException("Aucune grille tarifaire AUTO n'existe en base.");
        }
        toutes.forEach(this::charger);
        log.info("Grilles tarifaires AUTO contrôlées : {}", toutes.stream().map(VersionGrille::getVersion).toList());
    }

    private GrilleTarifaireAuto charger(VersionGrille version) {
        return assembleur.assembler(
                version,
                formules.findByGrilleIdOrderByOrdre(version.getId()),
                options.findByGrilleIdAndProduitOrderByCode(version.getId(), AUTO),
                tarifs.findByGrilleId(version.getId()));
    }
}
