package net.checkconsulting.assuranceiard.api.resource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.ContexteDeclarationResponse;
import net.checkconsulting.assuranceiard.api.dto.CreationSinistreRequest;
import net.checkconsulting.assuranceiard.api.dto.DeclarationSinistreResponse;
import net.checkconsulting.assuranceiard.api.dto.EnvoiSinistreRequest;
import net.checkconsulting.assuranceiard.api.dto.MiseAJourSinistreRequest;
import net.checkconsulting.assuranceiard.api.dto.SinistreEnvoyeResponse;
import net.checkconsulting.assuranceiard.api.dto.SinistreResumeResponse;
import net.checkconsulting.assuranceiard.api.dto.TypesSinistreResponse;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.securite.Acces;
import net.checkconsulting.assuranceiard.api.securite.TypeRessource;
import net.checkconsulting.assuranceiard.api.service.sinistre.DeclarationSinistreService;
import net.checkconsulting.assuranceiard.api.service.sinistre.EnvoiDeclarationService;
import net.checkconsulting.assuranceiard.api.service.sinistre.PiecesJointesSinistreService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * US-24 : déclarer un sinistre habitation ou auto. Rubrique SINISTRES (Assuré : consulter et créer, V15) ; seul le
 * propriétaire du contrat ou du sinistre y a accès (US-04, refus tracé). Codes : 404 contrat ou déclaration
 * introuvable (brouillon expiré compris), 422 contrat qui ne permet pas de déclarer (AC1) ou pièce refusée (AC15),
 * 400 réponse invalide ou manquante, 409 déclaration déjà envoyée, 503 stockage des pièces indisponible (AC16).
 * Les journaux ne contiennent jamais les réponses du client (ni blessés, ni description).
 */
@RestController
@RequiredArgsConstructor
public class SinistreResource {

    private static final String RESULTAT_STATUT = "resultat.statut";

    private final DeclarationSinistreService declarations;
    private final EnvoiDeclarationService envois;
    private final PiecesJointesSinistreService pieces;

    /** AC1, AC10 : contrats ouverts à la déclaration et numéro d'assistance. */
    @GetMapping("/sinistres/declaration")
    @Acces(rubrique = Rubrique.SINISTRES, droit = Droit.CREER)
    @LogMetier
    public ContexteDeclarationResponse contexte(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return declarations.contexte(authorization);
    }

    /** AC2, AC7 : types de sinistres du produit du contrat et pièces attendues. */
    @GetMapping("/contrats/{numero}/types-sinistre")
    @Acces(rubrique = Rubrique.SINISTRES, droit = Droit.CREER,
            ressource = TypeRessource.CONTRAT_NUMERO, identifiant = "numero")
    @LogMetier(champs = {"resultat.produit"})
    public TypesSinistreResponse types(@PathVariable String numero) {
        return declarations.types(numero);
    }

    /** AC8 : « Mes sinistres », dossiers et brouillons. */
    @GetMapping("/sinistres")
    @Acces(rubrique = Rubrique.SINISTRES, droit = Droit.CONSULTER)
    @LogMetier(champs = {"resultat.taille"})
    public List<SinistreResumeResponse> mesSinistres(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return declarations.mesSinistres(authorization);
    }

    /** AC1, AC2, AC8 : crée le brouillon (contrat et type choisis). */
    @PostMapping("/sinistres")
    @Acces(rubrique = Rubrique.SINISTRES, droit = Droit.CREER,
            ressource = TypeRessource.CONTRAT_NUMERO, identifiant = "demande.numeroContrat")
    @LogMetier(champs = {"resultat.etape"})
    public DeclarationSinistreResponse creer(@RequestBody CreationSinistreRequest demande) {
        return declarations.creer(demande);
    }

    @GetMapping("/sinistres/{id}")
    @Acces(rubrique = Rubrique.SINISTRES, droit = Droit.CONSULTER, ressource = TypeRessource.SINISTRE,
            identifiant = "id")
    @LogMetier(champs = {RESULTAT_STATUT})
    public DeclarationSinistreResponse lire(@PathVariable Long id) {
        return declarations.lire(id);
    }

    /** AC4 à AC13 : enregistre une étape du brouillon. */
    @PutMapping("/sinistres/{id}")
    @Acces(rubrique = Rubrique.SINISTRES, droit = Droit.CREER, ressource = TypeRessource.SINISTRE,
            identifiant = "id")
    @LogMetier(champs = {"resultat.etape"})
    public DeclarationSinistreResponse mettreAJour(@PathVariable Long id,
            @RequestBody MiseAJourSinistreRequest demande) {
        return declarations.mettreAJour(id, demande);
    }

    /** AC9 : envoie la déclaration (déclaration sur l'honneur cochée). */
    @PostMapping("/sinistres/{id}/envoi")
    @Acces(rubrique = Rubrique.SINISTRES, droit = Droit.CREER, ressource = TypeRessource.SINISTRE,
            identifiant = "id")
    @LogMetier(champs = {RESULTAT_STATUT, "resultat.piecesManquantes"})
    public SinistreEnvoyeResponse envoyer(@PathVariable Long id, @RequestBody(required = false)
            EnvoiSinistreRequest demande) {
        return envois.envoyer(id, demande);
    }

    /** AC7, AC14 à AC16 : dépose un justificatif. */
    @PostMapping(path = "/sinistres/{id}/pieces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Acces(rubrique = Rubrique.SINISTRES, droit = Droit.CREER, ressource = TypeRessource.SINISTRE,
            identifiant = "id")
    @LogMetier(champs = {RESULTAT_STATUT})
    public DeclarationSinistreResponse deposer(@PathVariable Long id, @RequestParam("typePiece") String typePiece,
            @RequestParam("fichier") MultipartFile fichier) {
        return pieces.deposer(id, typePiece, fichier);
    }

    /** AC14 : supprime un justificatif d'un brouillon. */
    @DeleteMapping("/sinistres/{id}/pieces/{pieceId}")
    @Acces(rubrique = Rubrique.SINISTRES, droit = Droit.CREER, ressource = TypeRessource.SINISTRE,
            identifiant = "id")
    @LogMetier(champs = {RESULTAT_STATUT})
    public DeclarationSinistreResponse supprimer(@PathVariable Long id, @PathVariable Long pieceId) {
        return pieces.supprimer(id, pieceId);
    }

    /** Télécharge un justificatif déposé (empreinte vérifiée). */
    @GetMapping("/sinistres/{id}/pieces/{pieceId}")
    @Acces(rubrique = Rubrique.SINISTRES, droit = Droit.CONSULTER, ressource = TypeRessource.SINISTRE,
            identifiant = "id")
    @LogMetier
    public ResponseEntity<byte[]> telecharger(@PathVariable Long id, @PathVariable Long pieceId) {
        PiecesJointesSinistreService.Fichier fichier = pieces.lire(id, pieceId);
        return ResponseEntity.ok()
                .contentType(typeMedia(fichier.format()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(fichier.nom(), StandardCharsets.UTF_8).build().toString())
                .body(fichier.contenu());
    }

    static MediaType typeMedia(String format) {
        return switch (format.toUpperCase(Locale.ROOT)) {
            case "PDF" -> MediaType.APPLICATION_PDF;
            case "PNG" -> MediaType.IMAGE_PNG;
            default -> MediaType.IMAGE_JPEG;
        };
    }
}
