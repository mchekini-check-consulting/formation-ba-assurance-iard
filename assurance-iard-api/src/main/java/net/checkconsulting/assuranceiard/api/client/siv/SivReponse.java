package net.checkconsulting.assuranceiard.api.client.siv;

/** US-27 : réponse normalisée du SIV (ou de son simulateur) pour une consultation par plaque. */
public record SivReponse(String statut, VehiculeInfoSiv vehicule) {

    public static final String TROUVE = "TROUVE";
    public static final String INCONNU = "INCONNU";
    public static final String ERREUR = "ERREUR";
    public static final String DELAI = "DELAI";
}
