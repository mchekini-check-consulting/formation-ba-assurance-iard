package net.checkconsulting.assuranceiard.api.service.sinistre;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.ContratOption;
import net.checkconsulting.assuranceiard.api.repository.ContratOptionRepository;
import net.checkconsulting.assuranceiard.api.repository.ReferentielSinistresRepository.TypeSinistre;
import org.springframework.stereotype.Component;

/**
 * US-24, AC3 et RG4 : le type de sinistre est-il couvert par les garanties du contrat <strong>à la date du
 * sinistre</strong> ? Formule du contrat (un avenant d'US-23 ne change pas la formule) et options en vigueur ce
 * jour-là (contrat_option : date de début et de fin, y compris après un avenant). Message seulement, jamais bloquant.
 */
@Component
@RequiredArgsConstructor
public class CouvertureContrat {

    private final ContratOptionRepository options;

    /** Vide pour « Autre » ; sinon vrai si la formule ou une option en vigueur à cette date couvre le type. */
    public Optional<Boolean> couvert(Contrat contrat, TypeSinistre type, LocalDate date) {
        List<String> codes = options.aLaDate(contrat.getId(), date).stream().map(ContratOption::getCodeOption)
                .toList();
        return ReglesDeclaration.couvert(type, contrat.getFormule(), codes);
    }
}
