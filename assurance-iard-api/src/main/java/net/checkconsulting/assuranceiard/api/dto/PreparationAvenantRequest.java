package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * US-23, AC3 : les garanties que le client veut avoir après l'avenant (codes), obligatoires comprises.
 */
public record PreparationAvenantRequest(@NotNull @Size(max = 20) List<@NotBlank @Size(max = 30) String> garanties) {
}
