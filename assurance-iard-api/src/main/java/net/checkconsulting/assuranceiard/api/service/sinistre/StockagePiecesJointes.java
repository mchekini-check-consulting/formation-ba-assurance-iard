package net.checkconsulting.assuranceiard.api.service.sinistre;

import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.PieceJointe;

/**
 * US-24, commentaire du 20/11/2026 : « Bouchon · Stockage des pièces jointes » (remplace la GED). Entrée : la pièce
 * (déjà contrôlée) et son fichier ; sortie : la clé de stockage, ou une erreur technique (AC16).
 */
public interface StockagePiecesJointes {

    /**
     * @return clé de stockage de la pièce
     * @throws net.checkconsulting.assuranceiard.api.exception.StockageIndisponibleException si le stockage ne répond
     *     pas
     */
    String stocker(PieceJointe piece, byte[] contenu);

    Optional<byte[]> lire(PieceJointe piece);
}
