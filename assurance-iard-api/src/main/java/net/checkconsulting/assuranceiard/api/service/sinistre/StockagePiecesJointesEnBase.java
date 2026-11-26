package net.checkconsulting.assuranceiard.api.service.sinistre;

import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.config.DonneesTestProperties;
import net.checkconsulting.assuranceiard.api.exception.StockageIndisponibleException;
import net.checkconsulting.assuranceiard.api.model.PieceJointe;
import net.checkconsulting.assuranceiard.api.model.PieceJointeContenu;
import net.checkconsulting.assuranceiard.api.repository.PieceJointeContenuRepository;
import org.springframework.stereotype.Component;

/**
 * US-24 : bouchon du stockage des pièces jointes. Le fichier est conservé en base (table piece_jointe_contenu), jamais
 * sur le disque du backend, vidé à chaque mise en ligne (IA-241). Clé : {@code base:piece_jointe_contenu/<id>}.
 *
 * <p>Comme le prévoit le tableau des fichiers de test de l'US (AC16), un fichier nommé « erreur-stockage… » simule
 * une erreur technique du stockage, seulement quand les données de test sont actives (local, dev, qualif ; jamais en
 * preprod ni en prod). Aucun service externe n'est appelé (RG12) : pas de WireMock dans ce dépôt.
 */
@Component
@RequiredArgsConstructor
public class StockagePiecesJointesEnBase implements StockagePiecesJointes {

    static final String FICHIER_EN_ERREUR = "erreur-stockage";
    static final String PREFIXE_CLE = "base:piece_jointe_contenu/";

    private final PieceJointeContenuRepository contenus;
    private final DonneesTestProperties donneesTest;

    @Override
    public String stocker(PieceJointe piece, byte[] contenu) {
        if (donneesTest.actif() && piece.getNomFichier().toLowerCase(Locale.ROOT).startsWith(FICHIER_EN_ERREUR)) {
            throw new StockageIndisponibleException();
        }
        PieceJointeContenu ligne = new PieceJointeContenu();
        ligne.setPieceJointeId(piece.getId());
        ligne.setContenu(contenu);
        ligne.setTailleOctets(contenu.length);
        contenus.save(ligne);
        return PREFIXE_CLE + piece.getId();
    }

    @Override
    public Optional<byte[]> lire(PieceJointe piece) {
        return contenus.findByPieceJointeId(piece.getId()).map(PieceJointeContenu::getContenu);
    }
}
