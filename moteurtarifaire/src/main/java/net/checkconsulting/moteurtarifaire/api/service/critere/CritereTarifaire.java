package net.checkconsulting.moteurtarifaire.api.service.critere;

import java.math.BigDecimal;
import net.checkconsulting.moteurtarifaire.api.dto.ProfilRisque;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire.Multiplicateurs;

public interface CritereTarifaire {

    BigDecimal NEUTRE = BigDecimal.ONE;

    BigDecimal multiplicateur(ProfilRisque profil, Multiplicateurs grille);
}
