package net.checkconsulting.moteurtarifaire.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.checkconsulting.moteurtarifaire.api.enums.Dependances;
import net.checkconsulting.moteurtarifaire.api.enums.Etage;
import net.checkconsulting.moteurtarifaire.api.enums.Formule;
import net.checkconsulting.moteurtarifaire.api.enums.OptionTarifaire;
import net.checkconsulting.moteurtarifaire.api.enums.RaisonRecommandation;
import net.checkconsulting.moteurtarifaire.api.enums.Residence;
import net.checkconsulting.moteurtarifaire.api.enums.ResiliationAnterieure;
import net.checkconsulting.moteurtarifaire.api.enums.StatutOccupant;
import net.checkconsulting.moteurtarifaire.api.enums.TypeLogement;

public record GrilleTarifaire(
        @NotBlank String version,
        @NotNull LocalDate dateDebut,
        LocalDate dateFin,
        @NotNull @Positive Integer dureeValiditeJours,
        @NotNull @Valid Arrondi arrondi,
        @NotEmpty Map<Formule, @Positive BigDecimal> prixBase,
        @NotNull @Valid CoefficientDepart coefficientDepart,
        @NotNull @Valid Multiplicateurs multiplicateurs,
        @NotEmpty Map<OptionTarifaire, @PositiveOrZero BigDecimal> prixOptions,
        @NotEmpty Map<Integer, @NotNull BigDecimal> ajustementsFranchise,
        @NotNull @Valid Refus refus,
        @NotNull @Valid ReglesRecommandation recommandation) {

    public record Arrondi(@NotNull @PositiveOrZero Integer decimales, @NotNull RoundingMode mode) {
    }

    public record CoefficientDepart(
            @NotNull @Positive BigDecimal surfaceReference,
            @NotNull @Positive BigDecimal surfacePlafond,
            @NotNull BigDecimal penteSurface,
            @NotNull @Positive BigDecimal coefficientSurfaceMinimum,
            @NotNull @Positive Integer piecesReference,
            @NotNull BigDecimal incrementParPiece) {
    }

    public record Multiplicateurs(
            @NotEmpty Map<StatutOccupant, @Positive BigDecimal> statut,
            @NotEmpty Map<TypeLogement, @Positive BigDecimal> typeLogement,
            @NotEmpty Map<Residence, @Positive BigDecimal> residence,
            @NotEmpty Map<Integer, @Positive BigDecimal> sinistres,
            @NotEmpty List<@Valid Zone> zones,
            @NotEmpty Map<Etage, @Positive BigDecimal> etage,
            @NotEmpty Map<Dependances, @Positive BigDecimal> dependances) {
    }

    public record Zone(@NotNull @Positive BigDecimal coefficient, @NotEmpty List<@NotBlank String> departements) {
    }

    public record Refus(
            @NotNull Map<StatutOccupant, @NotBlank String> statuts,
            @NotNull Map<ResiliationAnterieure, @NotBlank String> resiliations) {
    }

    public record ReglesRecommandation(
            @NotNull @Positive BigDecimal seuilContenuSerenite,
            @NotNull @Positive BigDecimal seuilContenuEssentielle,
            @NotEmpty Map<RaisonRecommandation, @NotBlank String> raisons) {
    }
}
