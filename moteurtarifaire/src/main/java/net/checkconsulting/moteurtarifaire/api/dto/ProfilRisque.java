package net.checkconsulting.moteurtarifaire.api.dto;

import java.math.BigDecimal;
import lombok.Builder;
import net.checkconsulting.moteurtarifaire.api.enums.Dependances;
import net.checkconsulting.moteurtarifaire.api.enums.Etage;
import net.checkconsulting.moteurtarifaire.api.enums.Residence;
import net.checkconsulting.moteurtarifaire.api.enums.ResiliationAnterieure;
import net.checkconsulting.moteurtarifaire.api.enums.StatutOccupant;
import net.checkconsulting.moteurtarifaire.api.enums.TypeLogement;

@Builder(toBuilder = true)
public record ProfilRisque(
        StatutOccupant statut,
        Residence residence,
        TypeLogement typeLogement,
        String codePostal,
        BigDecimal surface,
        int pieces,
        Etage etage,
        Dependances dependances,
        int sinistres3ans,
        ResiliationAnterieure resiliationAnterieure) {
}
