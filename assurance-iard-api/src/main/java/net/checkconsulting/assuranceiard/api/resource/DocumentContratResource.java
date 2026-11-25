package net.checkconsulting.assuranceiard.api.resource;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.exception.DocumentIndisponibleException;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.securite.Acces;
import net.checkconsulting.assuranceiard.api.securite.AccesPublic;
import net.checkconsulting.assuranceiard.api.securite.TypeRessource;
import net.checkconsulting.assuranceiard.api.service.DocumentService;
import net.checkconsulting.assuranceiard.api.service.document.DocumentsGeneresService;
import net.checkconsulting.assuranceiard.api.service.document.ModeleIPIDauto;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * T-03 : téléchargement d'un document PDF généré. Le client ne télécharge que ses documents (refus tracé dans
 * acces_refuse par le contrôle d'accès d'US-04) ; le back-office selon ses droits. Branchée sur l'écran dans US-18.
 */
@RestController
@RequiredArgsConstructor
public class DocumentContratResource {

    private final DocumentsGeneresService service;
    private final DocumentRepository documents;

    @AccesPublic
    @GetMapping("/documents-contrat/ipid/{reference}")
    public ResponseEntity<byte[]> ipid(@PathVariable String reference) {
        if (!ModeleIPIDauto.FORMULES_AUTO.contains(reference)) {
            return ResponseEntity.notFound().<byte[]>build();
        }
        Document doc = documents.enVigueur(0L, TypeDocument.IPID.name(), reference)
                .orElseThrow(DocumentIndisponibleException::new);
        DocumentService.Fichier fichier = service.telecharger(doc.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fichier.nom(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(fichier.contenu());
    }

    @GetMapping("/documents-contrat/{id}")
    @Acces(rubrique = Rubrique.DOCUMENTS, droit = Droit.CONSULTER, ressource = TypeRessource.DOCUMENT_CONTRAT,
            identifiant = "id")
    @LogMetier
    public ResponseEntity<byte[]> telecharger(@PathVariable Long id) {
        DocumentService.Fichier fichier = service.telecharger(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fichier.nom(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(fichier.contenu());
    }
}
