package net.checkconsulting.assuranceiard.api.service;

/** US-23, AC4 : envoie au client son avenant accepté (PDF joint, support durable) avec un lien vers son contrat. */
public interface EnvoiAvenant {

    /**
     * @param email destinataire (le client propriétaire du contrat)
     * @param prenom prénom du client, pour la formule d'appel ; peut être vide
     * @param numeroContrat numéro du contrat
     * @param numeroAvenant numéro AV-AAAA-NNNNNN
     * @param nomFichier nom du PDF joint
     * @param pdf contenu du PDF
     */
    void envoyer(String email, String prenom, String numeroContrat, String numeroAvenant, String nomFichier,
            byte[] pdf);
}
