package net.checkconsulting.assuranceiard.api.dto;

import java.util.List;

/** US-04 : droits du profil (« RUBRIQUE:DROIT »), relus à chaque lecture de la session, pour adapter les écrans. */
public record SessionCouranteResponse(String email, String profil, List<String> droits) {
}
