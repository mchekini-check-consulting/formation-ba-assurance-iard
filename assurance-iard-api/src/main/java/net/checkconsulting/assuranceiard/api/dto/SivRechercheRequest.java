package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.NotBlank;

/** US-27 : recherche de véhicule par plaque d'immatriculation. */
public record SivRechercheRequest(@NotBlank String immatriculation) {
}
