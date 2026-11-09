package net.checkconsulting.assuranceiard.api.mapper;

import net.checkconsulting.assuranceiard.api.dto.RecommandationRequest;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface RecommandationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reference", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    RecommandationDevis versEntite(RecommandationRequest demande);
}
