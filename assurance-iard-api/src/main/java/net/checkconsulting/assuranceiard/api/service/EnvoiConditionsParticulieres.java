package net.checkconsulting.assuranceiard.api.service;

/** US-21, AC3 : envoie au client ses conditions particulières régénérées (PDF joint, support durable RG3). */
public interface EnvoiConditionsParticulieres {

    /**
     * @param email destinataire (le client propriétaire du contrat)
     * @param prenom prénom du client, pour la formule d'appel ; peut être vide
     * @param numeroContrat numéro du contrat
     * @param version numéro de la nouvelle version
     * @param nomFichier nom du PDF joint
     * @param pdf contenu du PDF
     */
    void envoyer(String email, String prenom, String numeroContrat, int version, String nomFichier, byte[] pdf);
}
