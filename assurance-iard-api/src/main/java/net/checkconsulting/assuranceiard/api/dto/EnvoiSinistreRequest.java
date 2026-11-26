package net.checkconsulting.assuranceiard.api.dto;

/** US-24, AC9, RG8 : la case de déclaration sur l'honneur, jamais pré-cochée. */
public record EnvoiSinistreRequest(Boolean declarationHonneur) {
}
