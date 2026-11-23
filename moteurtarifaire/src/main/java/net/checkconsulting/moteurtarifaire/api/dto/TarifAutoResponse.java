package net.checkconsulting.moteurtarifaire.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TarifAutoResponse(
        UUID idTarif,
        String versionGrille,
        boolean tarifable,
        String motif,
        String formuleConseillee,
        List<PrixFormule> prix,
        List<PrixOption> prixOptions,
        Detail detail,
        LocalDate dateValidite) {

    public record PrixFormule(String formule, BigDecimal mensuel, BigDecimal annuel) {
    }

    public record PrixOption(String option, BigDecimal prixMensuel) {
    }

    public record Detail(
            String formule,
            BigDecimal prixFormule,
            List<PrixOption> options,
            BigDecimal totalMensuel,
            BigDecimal totalAnnuel) {
    }
}
