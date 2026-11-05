package net.checkconsulting.assuranceiard.api.mapper;

import net.checkconsulting.assuranceiard.api.dto.CompteResponse;
import net.checkconsulting.assuranceiard.api.dto.IdentiteResponse;
import net.checkconsulting.assuranceiard.api.model.Compte;
import org.mapstruct.Mapper;

@Mapper
public interface CompteMapper {

    CompteResponse versReponse(Compte compte);

    IdentiteResponse versIdentite(Compte compte);
}
