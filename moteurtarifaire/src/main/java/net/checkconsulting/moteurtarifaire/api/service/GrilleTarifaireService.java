package net.checkconsulting.moteurtarifaire.api.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleInvalideException;
import net.checkconsulting.moteurtarifaire.api.mapper.GrilleTarifaireAssembleur;
import net.checkconsulting.moteurtarifaire.api.model.VersionGrille;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire;
import net.checkconsulting.moteurtarifaire.api.repository.ParametreTarifaireRepository;
import net.checkconsulting.moteurtarifaire.api.repository.VersionGrilleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrilleTarifaireService {

    private final VersionGrilleRepository versions;
    private final ParametreTarifaireRepository parametres;
    private final GrilleTarifaireAssembleur assembleur;
    private final ValidateurGrille validateur;

    @Transactional(readOnly = true)
    public Optional<GrilleTarifaire> applicableAu(LocalDate date) {
        List<VersionGrille> applicables = versions.findApplicablesAu(date);
        if (applicables.isEmpty()) {
            log.warn("Aucune grille tarifaire en vigueur au {}", date);
        }
        if (applicables.size() > 1) {
            log.error("Plusieurs grilles tarifaires en vigueur au {}", date);
            throw new GrilleInvalideException("Plusieurs grilles tarifaires sont en vigueur au " + date + ".");
        }
        return applicables.stream().findFirst().map(this::charger);
    }

    @Transactional(readOnly = true)
    public void verifierToutes() {
        List<VersionGrille> toutes = versions.findAllByOrderByDateDebutAsc();
        if (toutes.isEmpty()) {
            throw new GrilleInvalideException("Aucune grille tarifaire n'existe en base.");
        }
        toutes.forEach(this::charger);
        verifierPeriodes(toutes);
        log.info("Grilles tarifaires contrôlées : {}", toutes.stream().map(VersionGrille::getVersion).toList());
    }

    private GrilleTarifaire charger(VersionGrille version) {
        GrilleTarifaire grille = assembleur.assembler(version, parametres.findByGrilleId(version.getId()));
        validateur.verifier(grille);
        return grille;
    }

    private void verifierPeriodes(List<VersionGrille> triees) {
        for (int i = 1; i < triees.size(); i++) {
            VersionGrille precedente = triees.get(i - 1);
            VersionGrille suivante = triees.get(i);
            if (precedente.getDateFin() == null || !precedente.getDateFin().isBefore(suivante.getDateDebut())) {
                throw new GrilleInvalideException("Les périodes des grilles " + precedente.getVersion()
                        + " et " + suivante.getVersion() + " se chevauchent.");
            }
        }
    }
}
