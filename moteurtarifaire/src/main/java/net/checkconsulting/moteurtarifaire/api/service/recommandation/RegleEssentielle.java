package net.checkconsulting.moteurtarifaire.api.service.recommandation;

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
@Order(2)
public class RegleEssentielle implements RegleRecommandation {

    @Override
    public Optional<FormuleConseillee> evaluer(Besoins besoins, ReglesRecommandation regles) {
        boolean applicable = besoins.valeurContenu().compareTo(regles.seuilContenuEssentielle()) < 0
                && besoins.priorite() == Priorite.PRIX
                && !besoins.objetsValeur();
        if (!applicable) {
            return Optional.empty();
        }
        return Optional.of(new FormuleConseillee(Formule.ESSENTIELLE, List.of(
                regles.raisons().get(RaisonRecommandation.CONTENU_FAIBLE),
                regles.raisons().get(RaisonRecommandation.AUCUN_OBJET_VALEUR),
                regles.raisons().get(RaisonRecommandation.PRIORITE_PRIX))));
    }
}
