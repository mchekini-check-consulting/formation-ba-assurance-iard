package net.checkconsulting.assuranceiard.api.client.siv;

/** US-27 : contrat d'accès au SIV (Système d'Immatriculation des Véhicules). Implémenté en prod par un client HTTP réel ; ici par {@link SivSimuleClient}. */
public interface SivClient {

    SivReponse consulterPlaque(String immatriculation);
}
