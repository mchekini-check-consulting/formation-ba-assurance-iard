package net.checkconsulting.assuranceiard.api.service.sinistre;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.DeclarationSinistreResponse;
import net.checkconsulting.assuranceiard.api.exception.DeclarationDejaEnvoyeeException;
import net.checkconsulting.assuranceiard.api.exception.DocumentAltereException;
import net.checkconsulting.assuranceiard.api.exception.DocumentInconnuException;
import net.checkconsulting.assuranceiard.api.exception.PieceRefuseeException;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.PieceJointe;
import net.checkconsulting.assuranceiard.api.model.Sinistre;
import net.checkconsulting.assuranceiard.api.repository.PieceJointeRepository;
import net.checkconsulting.assuranceiard.api.repository.ReferentielSinistresRepository;
import net.checkconsulting.assuranceiard.api.repository.ReferentielSinistresRepository.PieceAttendue;
import net.checkconsulting.assuranceiard.api.repository.SinistreRepository;
import net.checkconsulting.assuranceiard.api.utils.TypesFichier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * US-24, AC7, AC14 à AC16, RG7 : justificatifs d'une déclaration. PDF, JPG ou PNG, contrôlés sur leur contenu réel
 * (pas seulement l'extension), 10 Mo au plus par fichier, 20 fichiers par dossier, empreinte SHA-256 vérifiée à chaque
 * lecture. Le contrôle d'accès d'US-04 réserve ces routes au propriétaire du contrat. Une pièce peut être ajoutée
 * après l'envoi (« pièces manquantes » est alors recalculé) ; elle ne peut être supprimée que dans un brouillon.
 */
@Service
@RequiredArgsConstructor
public class PiecesJointesSinistreService {

    public static final long TAILLE_MAX = 10L * 1024 * 1024;
    static final int NOMBRE_MAX = 20;
    private static final int LONGUEUR_NOM = 255;

    private final DeclarationSinistreService declarations;
    private final SinistreRepository sinistres;
    private final PieceJointeRepository pieces;
    private final ReferentielSinistresRepository referentiel;
    private final StockagePiecesJointes stockage;
    private final VueDeclaration vue;

    /** Le fichier d'une pièce, prêt à être téléchargé. */
    public record Fichier(String nom, String format, byte[] contenu) {
    }

    /** AC14 à AC16 : dépose un fichier pour une pièce attendue ; renvoie la déclaration à jour. */
    @LogMetier(champs = {"resultat.piecesManquantes"})
    @Transactional
    public DeclarationSinistreResponse deposer(Long id, String typePiece, MultipartFile fichier) {
        Sinistre sinistre = declarations.visible(id);
        Contrat contrat = declarations.contratDe(sinistre);
        List<PieceAttendue> attendues = referentiel.pieces(contrat.getProduit(), sinistre.getType());
        if (attendues.stream().noneMatch(piece -> piece.code().equals(typePiece))) {
            throw new PieceRefuseeException(PieceRefuseeException.TYPE_PIECE);
        }
        if (pieces.countBySinistreIdAndStatut(id, VueDeclaration.STOCKEE) >= NOMBRE_MAX) {
            throw new PieceRefuseeException(PieceRefuseeException.NOMBRE);
        }
        byte[] contenu = contenu(fichier);
        String format = formatControle(fichier.getOriginalFilename(), contenu);

        PieceJointe piece = new PieceJointe();
        piece.setSinistreId(id);
        piece.setTypePiece(typePiece);
        piece.setNomFichier(nom(fichier.getOriginalFilename(), format));
        piece.setFormat(format.toUpperCase(Locale.ROOT));
        piece.setTailleOctets(contenu.length);
        piece.setEmpreinte(empreinte(contenu));
        PieceJointe enregistree = pieces.saveAndFlush(piece);
        enregistree.setCleStockage(stockage.stocker(enregistree, contenu));
        pieces.save(enregistree);
        return aJour(sinistre, contrat, attendues);
    }

    /** AC14 : « Supprimer » une pièce d'un brouillon. */
    @LogMetier(champs = {"resultat.piecesManquantes"})
    @Transactional
    public DeclarationSinistreResponse supprimer(Long id, Long pieceId) {
        Sinistre sinistre = declarations.visible(id);
        if (!DeclarationSinistreService.BROUILLON.equals(sinistre.getStatut())) {
            throw new DeclarationDejaEnvoyeeException();
        }
        PieceJointe piece = pieces.findByIdAndSinistreId(pieceId, id).orElseThrow(DocumentInconnuException::new);
        pieces.delete(piece);
        pieces.flush();
        Contrat contrat = declarations.contratDe(sinistre);
        return aJour(sinistre, contrat, referentiel.pieces(contrat.getProduit(), sinistre.getType()));
    }

    /** Le fichier déposé, seulement s'il n'a pas été altéré (empreinte SHA-256). */
    @LogMetier
    @Transactional(readOnly = true)
    public Fichier lire(Long id, Long pieceId) {
        declarations.visible(id);
        PieceJointe piece = pieces.findByIdAndSinistreId(pieceId, id).orElseThrow(DocumentInconnuException::new);
        byte[] contenu = stockage.lire(piece).orElseThrow(DocumentInconnuException::new);
        if (piece.getEmpreinte() != null && !piece.getEmpreinte().equals(empreinte(contenu))) {
            throw new DocumentAltereException();
        }
        return new Fichier(piece.getNomFichier(), piece.getFormat(), contenu);
    }

    /** AC15 : PDF, JPG ou PNG d'après le contenu réel, et extension cohérente. */
    static String formatControle(String nomFichier, byte[] contenu) {
        String reel = TypesFichier.typeReel(contenu).orElseThrow(() -> new PieceRefuseeException(
                PieceRefuseeException.FORMAT));
        if (!TypesFichier.extensionAcceptee(nomFichier) || !reel.equals(extension(nomFichier))) {
            throw new PieceRefuseeException(PieceRefuseeException.FORMAT);
        }
        return reel;
    }

    /** Empreinte SHA-256 (hexadécimal) du fichier. */
    public static String empreinte(byte[] contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
        } catch (NoSuchAlgorithmException erreur) {
            throw new IllegalStateException("SHA-256 indisponible", erreur);
        }
    }

    /** Nom affiché : sans chemin, 255 caractères au plus, l'extension étant gardée. */
    static String nom(String nomFichier, String format) {
        String nom = nomFichier == null ? "" : nomFichier.replaceAll("^.*[/\\\\]", "").strip();
        if (nom.isEmpty()) {
            return "piece." + format;
        }
        return nom.length() <= LONGUEUR_NOM ? nom : nom.substring(nom.length() - LONGUEUR_NOM);
    }

    private static String extension(String nomFichier) {
        String extension = nomFichier.substring(nomFichier.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return "jpeg".equals(extension) ? "jpg" : extension;
    }

    private static byte[] contenu(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new PieceRefuseeException(PieceRefuseeException.VIDE);
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw new PieceRefuseeException(PieceRefuseeException.TAILLE);
        }
        try {
            return fichier.getBytes();
        } catch (IOException erreur) {
            throw new UncheckedIOException("Lecture du fichier déposé impossible", erreur);
        }
    }

    /** AC7 : après un dépôt ou une suppression, « pièces manquantes » d'un dossier envoyé est recalculé. */
    private DeclarationSinistreResponse aJour(Sinistre sinistre, Contrat contrat, List<PieceAttendue> attendues) {
        if (DeclarationSinistreService.DECLARE.equals(sinistre.getStatut())) {
            sinistre.setPiecesManquantes(vue.piecesManquantes(sinistre, attendues));
            sinistres.save(sinistre);
        }
        return vue.de(sinistre, contrat, DeclarationSinistreService.prenom(contrat));
    }
}
