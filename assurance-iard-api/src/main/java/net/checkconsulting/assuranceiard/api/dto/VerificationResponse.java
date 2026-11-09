package net.checkconsulting.assuranceiard.api.dto;

/** verification : jeton à renvoyer avec le code ; emailMasque : adresse à laquelle le code a été envoyé. */
public record VerificationResponse(String verification, String emailMasque) {
}
