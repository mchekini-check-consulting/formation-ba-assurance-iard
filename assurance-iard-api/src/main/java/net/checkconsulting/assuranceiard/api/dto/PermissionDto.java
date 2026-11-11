package net.checkconsulting.assuranceiard.api.dto;

public record PermissionDto(
        String rubrique,
        String droit,
        boolean valeur
) {
}
