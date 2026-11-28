package net.checkconsulting.assuranceiard.api.service.hamon;

import java.time.LocalDate;

/** US-33 (§8) : résiliation de l'ancien contrat par la loi Hamon. */
public interface HamonService {

    /**
     * Enregistre la demande de résiliation Hamon et simule la réponse de l'assureur.
     * N'échoue jamais (les erreurs sont journalisées dans resiliation_hamon).
     * Ne change jamais le statut du contrat en cas d'erreur.
     */
    void resilier(long contratId, String ancienAssureur, String ancienNumero, LocalDate dateEffet);
}
