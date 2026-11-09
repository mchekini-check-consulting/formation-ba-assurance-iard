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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class RegleConfort implements RegleRecommandation {

    @Override
    public Optional<FormuleConseillee> evaluer(Besoins besoins, ReglesRecommandation regles) {
        List<String> raisons = new ArrayList<>();
        if (besoins.valeurContenu().compareTo(regles.seuilContenuEssentielle()) >= 0) {
            raisons.add(regles.raisons().get(RaisonRecommandation.CONTENU_MOYEN));
        }
        if (besoins.objetsValeur()) {
            raisons.add(regles.raisons().get(RaisonRecommandation.OBJETS_VALEUR));
        }
        if (besoins.priorite() == Priorite.EQUILIBRE) {
            raisons.add(regles.raisons().get(RaisonRecommandation.PRIORITE_EQUILIBRE));
        }
        return Optional.of(new FormuleConseillee(Formule.CONFORT, raisons));
    }
}
