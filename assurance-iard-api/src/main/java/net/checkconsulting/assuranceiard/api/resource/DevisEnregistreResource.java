package net.checkconsulting.assuranceiard.api.resource;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.DevisEnregistreResponse;
import net.checkconsulting.assuranceiard.api.dto.DevisRequest;
import net.checkconsulting.assuranceiard.api.dto.RepriseDevisResponse;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.securite.Acces;
import net.checkconsulting.assuranceiard.api.securite.AccesPublic;
import net.checkconsulting.assuranceiard.api.securite.TypeRessource;
import net.checkconsulting.assuranceiard.api.service.DevisEnregistreService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DevisEnregistreResource {

    private static final String NOM_PDF = "devis-assureasy.pdf";

    private final DevisEnregistreService service;

    @PostMapping("/devis/enregistres")
    @Acces(rubrique = Rubrique.DEVIS, droit = Droit.CREER)
    @LogMetier(champs = {"demande.formule", "demande.franchise", "demande.options"})
    public DevisEnregistreResponse enregistrer(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody DevisRequest demande) {
        return service.enregistrer(authorization, demande);
    }

    @GetMapping("/devis/enregistres")
    @Acces(rubrique = Rubrique.DEVIS, droit = Droit.CONSULTER)
    @LogMetier(champs = {"resultat.taille"})
    public List<DevisEnregistreResponse> lister(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return service.lister(authorization);
    }

    @PostMapping("/devis/enregistres/{reference}/reprise")
    @Acces(rubrique = Rubrique.DEVIS, droit = Droit.MODIFIER, ressource = TypeRessource.DEVIS, identifiant = "reference")
    @LogMetier(champs = {"resultat.formule", "resultat.franchise", "resultat.prixMensuel"})
    public RepriseDevisResponse reprendre(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID reference) {
        return service.reprendre(authorization, reference);
    }

    /** RGPD : supprimer son devis relève de « Modifier » (le droit « Désactiver » n'est pas donné aux clients). */
    @DeleteMapping("/devis/enregistres/{reference}")
    @Acces(rubrique = Rubrique.DEVIS, droit = Droit.MODIFIER, ressource = TypeRessource.DEVIS, identifiant = "reference")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @LogMetier
    public void supprimer(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID reference) {
        service.supprimer(authorization, reference);
    }

    /** Devis sans compte (RG2 de l'US-12). */
    @PostMapping("/devis/pdf")
    @AccesPublic
    public ResponseEntity<byte[]> telecharger(@Valid @RequestBody DevisRequest demande) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(NOM_PDF).build().toString())
                .body(service.telecharger(demande));
    }
}
