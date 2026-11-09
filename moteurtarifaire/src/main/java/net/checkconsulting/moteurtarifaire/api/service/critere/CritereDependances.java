package net.checkconsulting.moteurtarifaire.api.service.critere;

import java.math.BigDecimal;
import net.checkconsulting.moteurtarifaire.api.dto.ProfilRisque;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire.Multiplicateurs;
import net.checkconsulting.moteurtarifaire.api.enums.TypeLogement;
import org.springframework.stereotype.Component;

@Component
public class CritereDependances implements CritereTarifaire {

    @Override
    public BigDecimal multiplicateur(ProfilRisque profil, Multiplicateurs grille) {
        return profil.typeLogement() == TypeLogement.MAISON ? grille.dependances().get(profil.dependances()) : NEUTRE;
    }
}
