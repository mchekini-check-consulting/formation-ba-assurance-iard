package net.checkconsulting.assuranceiard.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** US-29 étape 3 : prix des 3 formules auto et des options, calculés par le moteur tarifaire. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PrixFormuleAutoResponse(
        UUID idTarif,
        String versionGrille,
        LocalDate dateValidite,
        List<PrixFormule> prix,
        List<PrixOption> prixOptions) {

    /** formule = code moteur (TIERS / INTER / TR) ; franchise null = sans objet. */
    public record PrixFormule(String formule, BigDecimal mensuel, BigDecimal annuel, BigDecimal franchise) {}

    public record PrixOption(String option, BigDecimal mensuel) {}
}
