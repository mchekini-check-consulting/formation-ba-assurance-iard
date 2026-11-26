package net.checkconsulting.assuranceiard.api.service;

import java.time.LocalDate;

/**
 * US-24 : e-mails de la déclaration de sinistre (Mailpit en local, dev et qualif). Aucune donnée de santé (ni
 * blessés, ni description des faits) et aucune pièce jointe (RG commune « Données de santé », RGPD article 9).
 */
public interface EnvoiSinistre {

    /**
     * Ce que les e-mails disent du dossier.
     *
     * @param email adresse du souscripteur (propriétaire du contrat)
     * @param prenom prénom du souscripteur ; peut être vide
     * @param piecesManquantes AC7 : une pièce obligatoire reste à envoyer
     * @param declarantNom AC13 : nom de la personne qui a déclaré pour l'assuré ; vide si c'est l'assuré
     * @param declarantLien AC13 : son lien avec l'assuré
     */
    record Dossier(String email, String prenom, String numeroSinistre, String numeroContrat, String libelleType,
            LocalDate dateSurvenance, boolean piecesManquantes, String declarantNom, String declarantLien) {
    }

    /** AC9 : accusé de réception du dossier. */
    void accuserReception(Dossier dossier);

    /** AC13 : informe le souscripteur qu'une autre personne a déclaré pour lui. */
    void informerSouscripteur(Dossier dossier);
}
