package net.checkconsulting.assuranceiard.api.resource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.DocumentResponse;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.securite.Acces;
import net.checkconsulting.assuranceiard.api.securite.TypeRessource;
import net.checkconsulting.assuranceiard.api.service.DocumentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** US-04 : « Mes documents » (pièces déposées à l'US-13). */
@RestController
@RequiredArgsConstructor
public class DocumentResource {

    private final DocumentService documentService;

    @GetMapping("/documents")
    @Acces(rubrique = Rubrique.DOCUMENTS, droit = Droit.CONSULTER)
    @LogMetier(champs = {"resultat.taille"})
    public List<DocumentResponse> lister(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return documentService.lister(authorization);
    }

    @GetMapping("/documents/{id}")
    @Acces(rubrique = Rubrique.DOCUMENTS, droit = Droit.CONSULTER, ressource = TypeRessource.DOCUMENT, identifiant = "id")
    @LogMetier
    public ResponseEntity<byte[]> ouvrir(@PathVariable Long id) {
        DocumentService.Fichier fichier = documentService.ouvrir(id);
        return ResponseEntity.ok()
                .contentType(typeMedia(fichier.type()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(fichier.nom(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(fichier.contenu());
    }

    static MediaType typeMedia(String type) {
        return switch (type) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg" -> MediaType.IMAGE_JPEG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
