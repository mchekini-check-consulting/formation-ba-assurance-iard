package net.checkconsulting.assuranceiard.api.dto;

/** US-23, AC4 : case « J'ai lu et j'accepte mon avenant » (RG1 : jamais pré-cochée à l'écran). */
public record AcceptationAvenantRequest(boolean accepte) {
}
