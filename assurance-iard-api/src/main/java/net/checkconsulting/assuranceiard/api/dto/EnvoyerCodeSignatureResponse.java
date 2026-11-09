package net.checkconsulting.assuranceiard.api.dto;

/** Réponse à l'envoi du code de signature : adresse masquée à afficher à l'écran (RG5). */
public record EnvoyerCodeSignatureResponse(String emailMasque) {
}
