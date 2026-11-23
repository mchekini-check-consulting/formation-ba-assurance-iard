package net.checkconsulting.moteurtarifaire.api.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaireAuto;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleInvalideException;
import net.checkconsulting.moteurtarifaire.api.model.RefFormule;
import net.checkconsulting.moteurtarifaire.api.model.RefOption;
import net.checkconsulting.moteurtarifaire.api.model.RefTarif;
import net.checkconsulting.moteurtarifaire.api.model.VersionGrille;
import org.springframework.stereotype.Component;

@Component
public class GrilleTarifaireAutoAssembleur {

    private static final int DUREE_VALIDITE_JOURS = 30;

    private static final List<String> CRITERES_REQUIS = List.of(
            "AGE", "BONUS_MALUS", "USAGE", "VALEUR_VEHICULE", "KM_ANNUEL", "STATIONNEMENT", "SURPRIME_NOVICE");

    public GrilleTarifaireAuto assembler(VersionGrille version, List<RefFormule> formules,
            List<RefOption> options, List<RefTarif> tarifs) {
        if (formules.isEmpty()) {
            throw new GrilleInvalideException("Grille AUTO " + version.getVersion() + " : aucune formule.");
        }
        Map<String, List<RefTarif>> parCritere = tarifs.stream()
                .collect(Collectors.groupingBy(RefTarif::getCritere));
        for (String critere : CRITERES_REQUIS) {
            if (!parCritere.containsKey(critere) || parCritere.get(critere).isEmpty()) {
                throw new GrilleInvalideException(
                        "Grille AUTO " + version.getVersion() + " incomplète : critère " + critere + " absent.");
            }
        }
        return new GrilleTarifaireAuto(
                version.getVersion(),
                version.getDateDebut(),
                version.getDateFin(),
                DUREE_VALIDITE_JOURS,
                formules,
                options,
                parCritere);
    }
}
