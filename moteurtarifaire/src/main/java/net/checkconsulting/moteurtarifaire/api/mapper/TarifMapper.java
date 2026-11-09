package net.checkconsulting.moteurtarifaire.api.mapper;

import net.checkconsulting.moteurtarifaire.api.dto.BesoinsRequest;
import net.checkconsulting.moteurtarifaire.api.dto.TarifHabitationRequest;
import net.checkconsulting.moteurtarifaire.api.dto.Besoins;
import net.checkconsulting.moteurtarifaire.api.dto.ProfilRisque;
import org.mapstruct.Mapper;

@Mapper
public interface TarifMapper {

    ProfilRisque versProfil(TarifHabitationRequest demande);

    Besoins versBesoins(BesoinsRequest besoins);
}
