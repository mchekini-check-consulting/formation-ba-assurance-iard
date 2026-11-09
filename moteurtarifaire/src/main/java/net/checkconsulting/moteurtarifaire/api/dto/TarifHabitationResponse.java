package net.checkconsulting.moteurtarifaire.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import net.checkconsulting.moteurtarifaire.api.enums.Formule;
import net.checkconsulting.moteurtarifaire.api.enums.OptionTarifaire;
import net.checkconsulting.moteurtarifaire.api.enums.Orientation;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TarifHabitationResponse(
        UUID idTarif,
        String versionGrille,
        boolean tarifable,
        String motif,
        Orientation orientation,
        List<PrixFormuleResponse> prix,
        DetailFormuleResponse detail,
        Formule formuleConseillee,
        List<String> raisons,
        List<OptionTarifaire> optionsConseillees,
        LocalDate dateValidite,
        List<PrixOptionResponse> prixOptions,
        List<AjustementFranchiseResponse> ajustementsFranchise) {
}
