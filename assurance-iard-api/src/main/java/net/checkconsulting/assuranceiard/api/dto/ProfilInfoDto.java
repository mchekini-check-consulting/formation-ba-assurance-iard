package net.checkconsulting.assuranceiard.api.dto;

import java.util.List;

public record ProfilInfoDto(
        String nom,
        String description,
        long nombreUtilisateurs,
        List<PermissionDto> permissions
) {
}
