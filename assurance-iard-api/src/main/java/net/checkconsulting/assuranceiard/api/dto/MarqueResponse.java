package net.checkconsulting.assuranceiard.api.dto;

import java.util.List;

/** US-27 : marque avec la liste de ses modèles dans le référentiel véhicule. */
public record MarqueResponse(String marque, List<ModeleResponse> modeles) {
}
