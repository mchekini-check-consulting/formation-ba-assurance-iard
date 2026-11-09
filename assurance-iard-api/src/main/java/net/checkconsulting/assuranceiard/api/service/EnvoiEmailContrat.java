package net.checkconsulting.assuranceiard.api.service;

/** Envoie le code de signature du contrat à l'adresse e-mail du souscripteur (US-16). */
public interface EnvoiEmailContrat {

    void envoyer(String destinataire, String code);
}
