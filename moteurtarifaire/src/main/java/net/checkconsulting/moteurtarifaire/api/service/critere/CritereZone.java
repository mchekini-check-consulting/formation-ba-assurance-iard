package net.checkconsulting.moteurtarifaire.api.service.critere;

import java.math.BigDecimal;
import net.checkconsulting.moteurtarifaire.api.dto.ProfilRisque;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire.Multiplicateurs;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire.Zone;
import org.springframework.stereotype.Component;

@Component
public class CritereZone implements CritereTarifaire {

    @Override
    public BigDecimal multiplicateur(ProfilRisque profil, Multiplicateurs grille) {
        return grille.zones().stream()
                .filter(zone -> zone.departements().stream().anyMatch(profil.codePostal()::startsWith))
                .map(Zone::coefficient)
                .findFirst()
                .orElse(NEUTRE);
    }
}
