package net.checkconsulting.assuranceiard.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MandatPreparationRequest(@NotNull UUID referenceDevis) {
}
