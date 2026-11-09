package net.checkconsulting.moteurtarifaire.api.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.moteurtarifaire.api.dto.Besoins;
import net.checkconsulting.moteurtarifaire.api.dto.FormuleConseillee;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire.ReglesRecommandation;
import net.checkconsulting.moteurtarifaire.api.enums.OptionTarifaire;
import net.checkconsulting.moteurtarifaire.api.service.recommandation.RegleRecommandation;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommandationService {

    private final List<RegleRecommandation> regles;

    public FormuleConseillee formuleConseillee(Besoins besoins, ReglesRecommandation reglesGrille) {
        FormuleConseillee conseil = regles.stream()
                .flatMap(regle -> regle.evaluer(besoins, reglesGrille).stream())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucune règle de recommandation n'a répondu."));
        log.info("Formule conseillée : formule={}, raisons={}", conseil.formule(), conseil.raisons().size());
        return conseil;
    }

    public List<OptionTarifaire> optionsConseillees(Besoins besoins) {
        List<OptionTarifaire> options = new ArrayList<>();
        if (besoins.objetsValeur()) {
            options.add(OptionTarifaire.OBJETS_VALEUR);
        }
        if (besoins.assistance()) {
            options.add(OptionTarifaire.ASSISTANCE);
        }
        if (besoins.litiges()) {
            options.add(OptionTarifaire.PROTECTION_JURIDIQUE);
        }
        return options;
    }
}
