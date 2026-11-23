package net.checkconsulting.moteurtarifaire.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.checkconsulting.moteurtarifaire.api.model.RefFormule;
import net.checkconsulting.moteurtarifaire.api.model.RefOption;
import net.checkconsulting.moteurtarifaire.api.model.RefTarif;

/**
 * Représentation en mémoire de la grille tarifaire AUTO chargée depuis la base.
 * Distincte de {@link GrilleTarifaire} (habitation) car la structure des données est différente.
 */
public record GrilleTarifaireAuto(
        String version,
        LocalDate dateDebut,
        LocalDate dateFin,
        int dureeValiditeJours,
        List<RefFormule> formules,
        List<RefOption> options,
        Map<String, List<RefTarif>> coefficientsParCritere) {
}
