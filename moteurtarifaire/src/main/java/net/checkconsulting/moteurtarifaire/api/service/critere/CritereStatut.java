package net.checkconsulting.moteurtarifaire.api.service.critere;

import java.math.BigDecimal;
import net.checkconsulting.moteurtarifaire.api.dto.ProfilRisque;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire.Multiplicateurs;
import org.springframework.stereotype.Component;

@Component
public class CritereStatut implements CritereTarifaire {

    @Override
    public BigDecimal multiplicateur(ProfilRisque profil, Multiplicateurs grille) {
        return grille.statut().get(profil.statut());
    }
}
