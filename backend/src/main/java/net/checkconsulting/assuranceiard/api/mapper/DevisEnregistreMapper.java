package net.checkconsulting.assuranceiard.api.mapper;

import net.checkconsulting.assuranceiard.api.dto.BesoinsDevis;
import net.checkconsulting.assuranceiard.api.dto.LogementDevis;
import net.checkconsulting.assuranceiard.api.model.DevisEnregistre;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring")
public interface DevisEnregistreMapper {

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void copierLogement(LogementDevis logement, @MappingTarget DevisEnregistre devis);

    LogementDevis versLogement(DevisEnregistre devis);

    BesoinsDevis versBesoins(RecommandationDevis recommandation);
}
