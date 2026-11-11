package net.checkconsulting.assuranceiard.api.dto;

/** US-04 : un client dans la liste du back-office. */
public record ClientResumeResponse(Long id, String prenom, String nom, String email, String profil) {
}
