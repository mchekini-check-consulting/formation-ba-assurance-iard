package net.checkconsulting.moteurtarifaire.api.service;

import java.util.Optional;
import net.checkconsulting.moteurtarifaire.api.dto.ProfilRisque;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire.Refus;
import org.springframework.stereotype.Service;

@Service
public class EligibiliteService {

    public Optional<String> motifDeRefus(ProfilRisque profil, Refus refus) {
        return Optional.ofNullable(refus.statuts().get(profil.statut()))
                .or(() -> Optional.ofNullable(refus.resiliations().get(profil.resiliationAnterieure())));
    }
}
