package net.checkconsulting.assuranceiard.api.dto;

import java.time.LocalDate;

public record IdentiteResponse(String civilite, String nom, String prenom, LocalDate dateNaissance, String telephone) {
}
