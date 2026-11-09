package net.checkconsulting.moteurtarifaire.api.service.recommandation;

import java.util.Optional;
import net.checkconsulting.moteurtarifaire.api.dto.Besoins;
import net.checkconsulting.moteurtarifaire.api.dto.FormuleConseillee;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire.ReglesRecommandation;

public interface RegleRecommandation {

    Optional<FormuleConseillee> evaluer(Besoins besoins, ReglesRecommandation regles);
}
