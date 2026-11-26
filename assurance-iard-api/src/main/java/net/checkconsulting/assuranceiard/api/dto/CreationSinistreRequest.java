package net.checkconsulting.assuranceiard.api.dto;

/** US-24, AC1 et AC2 : contrat et type choisis à l'étape 1 ; le brouillon est créé (AC8). */
public record CreationSinistreRequest(String numeroContrat, String type) {
}
