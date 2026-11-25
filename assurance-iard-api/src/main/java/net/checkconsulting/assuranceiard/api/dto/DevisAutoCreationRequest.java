package net.checkconsulting.assuranceiard.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** US-28 / US-59 : corps de la requête de création d'un devis auto (étape 1.4). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DevisAutoCreationRequest(

        // ── Véhicule (étapes 1.1 + 1.2) ──────────────────────────────────
        /** Identifiant dans ref_vehicule (obligatoire si immatriculation absente). */
        Long refVehiculeId,
        /** Plaque de test SIV (obligatoire si refVehiculeId absent). */
        String immatriculation,
        @NotNull Boolean nonImmatricule,
        String titulaireCarteGrise,
        String dateAcquisition,
        String modeAchat,

        // ── Usage (étape 1.3) ─────────────────────────────────────────────
        @NotBlank String usage,
        @NotBlank String stationnement,
        @NotNull @Min(1) @Max(200000) Integer kmAnnuels,
        @NotBlank String codePostal,

        // ── Conducteur principal (étape 1.4) ─────────────────────────────
        @NotBlank String conducteurPrincipal,
        @NotBlank String dateNaissance,
        @NotBlank String datePermis,
        @NotNull @DecimalMin("0.50") @DecimalMax("3.50") BigDecimal bonusMalus,
        @NotNull @Min(0) Integer anneesAssurance,
        @NotNull Boolean conduiteAccompagnee,

        // ── Antécédents ───────────────────────────────────────────────────
        @NotNull @Min(0) @Max(3) Integer nbSinistresResponsables,
        @NotNull @Min(0) @Max(3) Integer nbSinistresNonResponsables,
        @NotBlank String resiliationAssureur,
        @NotNull Boolean suspensionAnnulation,
        @NotNull Boolean condamnationAlcool,
        @NotBlank String assureVehicule,

        // ── Hamon (obligatoire si assureVehicule = OUI_PLUS_UN_AN) ───────
        String ancienAssureur,
        String ancienNumeroContrat) {
}
