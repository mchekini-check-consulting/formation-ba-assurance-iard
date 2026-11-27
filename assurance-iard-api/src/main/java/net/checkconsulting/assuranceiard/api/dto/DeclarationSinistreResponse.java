package net.checkconsulting.assuranceiard.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * US-24 : une déclaration (brouillon ou envoyée), avec ses contrôles recalculés à chaque lecture (AC3, AC5, AC6),
 * ses pièces attendues et déposées (AC7). Aucune donnée médicale (AC11).
 */
public record DeclarationSinistreResponse(
        Long id,
        String numero,
        String statut,
        int etape,
        String prenom,
        String numeroAssistance,
        ContexteDeclarationResponse.ContratDeclarable contrat,
        String type,
        String libelleType,
        List<String> gestesUrgence,
        LocalDate dateSurvenance,
        LocalTime heureSurvenance,
        String lieu,
        String description,
        Boolean tiersImplique,
        MiseAJourSinistreRequest.Tiers tiers,
        Boolean blesses,
        Integer nombreBlesses,
        String declarant,
        String declarantNom,
        String declarantLien,
        Map<String, String> details,
        LocalDate dateArreteCatnat,
        String explicationRetard,
        Controles controles,
        List<TypesSinistreResponse.PieceAttendue> piecesAttendues,
        List<PieceJointeResponse> piecesJointes,
        boolean piecesManquantes,
        Instant brouillonExpireLe,
        Instant declareLe) {

    /**
     * @param couvert AC3 : vide pour « Autre » (aucun message), faux si ni la formule ni les options en vigueur à la
     *     date du sinistre ne couvrent ce type (à la date du jour tant que la date du sinistre n'est pas connue)
     * @param horsPeriodeContrat AC5
     * @param horsDelai AC6 (explication du retard obligatoire à l'envoi)
     * @param delaiPrevu RG2, en toutes lettres
     * @param dateLimite RG2 : dernier jour pour déclarer ; vide si inconnu
     */
    public record Controles(Boolean couvert, boolean horsPeriodeContrat, boolean horsDelai, String delaiPrevu,
            LocalDate dateLimite) {
    }

    public record PieceJointeResponse(Long id, String typePiece, String nomFichier, String format, int tailleOctets,
            Instant deposeeLe) {
    }
}
