package net.checkconsulting.moteurtarifaire.api.service.recommandation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.moteurtarifaire.api.dto.Besoins;
import net.checkconsulting.moteurtarifaire.api.dto.FormuleConseillee;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire.ReglesRecommandation;
import net.checkconsulting.moteurtarifaire.api.enums.Formule;
import net.checkconsulting.moteurtarifaire.api.enums.Priorite;
import net.checkconsulting.moteurtarifaire.api.enums.RaisonRecommandation;
import net.checkconsulting.moteurtarifaire.api.enums.Remboursement;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class RegleSerenite implements RegleRecommandation {

    @Override
    public Optional<FormuleConseillee> evaluer(Besoins besoins, ReglesRecommandation regles) {
        List<String> raisons = new ArrayList<>();
        if (besoins.valeurContenu().compareTo(regles.seuilContenuSerenite()) > 0) {
            raisons.add(regles.raisons().get(RaisonRecommandation.CONTENU_ELEVE));
        }
        if (besoins.remboursement() == Remboursement.NEUF) {
            raisons.add(regles.raisons().get(RaisonRecommandation.REMBOURSEMENT_NEUF));
        }
        if (besoins.appareilsCouteux()) {
            raisons.add(regles.raisons().get(RaisonRecommandation.APPAREILS_COUTEUX));
        }
        if (besoins.priorite() == Priorite.PROTECTION) {
            raisons.add(regles.raisons().get(RaisonRecommandation.PRIORITE_PROTECTION));
        }
        return raisons.isEmpty() ? Optional.empty() : Optional.of(new FormuleConseillee(Formule.SERENITE, raisons));
    }
}
