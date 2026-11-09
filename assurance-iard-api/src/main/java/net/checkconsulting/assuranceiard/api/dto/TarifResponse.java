package net.checkconsulting.assuranceiard.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TarifResponse(
        UUID idTarif,
        String versionGrille,
        boolean tarifable,
        String motif,
        String orientation,
        List<PrixFormule> prix,
        Detail detail,
        String formuleConseillee,
        List<String> raisons,
        List<String> optionsConseillees,
        LocalDate dateValidite,
        List<PrixOption> prixOptions,
        List<AjustementFranchise> ajustementsFranchise) {

    public record PrixFormule(String formule, BigDecimal mensuel, BigDecimal annuel) {
    }

    public record PrixOption(String option, BigDecimal prixMensuel) {
    }

    public record AjustementFranchise(Integer franchise, BigDecimal ajustement) {
    }

    public record Detail(
            String formule,
            BigDecimal prixFormule,
            List<PrixOption> options,
            BigDecimal ajustementFranchise,
            BigDecimal totalMensuel,
            BigDecimal totalAnnuel) {
    }
}
