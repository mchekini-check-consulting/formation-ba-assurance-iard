package net.checkconsulting.assuranceiard.api.dto;

import java.time.LocalDate;

public record MandatSigneResponse(String rum, LocalDate datePremierPrelevement) {
}
