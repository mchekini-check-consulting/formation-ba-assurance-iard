package net.checkconsulting.assuranceiard.api.service.fva;

/** US-33 (§7) : déclaration d'un véhicule au fichier des véhicules assurés (FVA). */
public interface FvaService {

    /**
     * Déclare le véhicule au FVA après la signature du contrat.
     * N'échoue jamais (les erreurs sont journalisées et le statut est mis à jour dans declaration_fva).
     */
    void declarer(long contratId, String immatriculation);
}
