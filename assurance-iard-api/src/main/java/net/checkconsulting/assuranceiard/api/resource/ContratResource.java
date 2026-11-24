package net.checkconsulting.assuranceiard.api.resource;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.ContratResponse;
import net.checkconsulting.assuranceiard.api.dto.ContratResumeResponse;
import net.checkconsulting.assuranceiard.api.dto.EnvoyerCodeSignatureResponse;
import net.checkconsulting.assuranceiard.api.dto.FicheContratResponse;
import net.checkconsulting.assuranceiard.api.dto.GenerationContratRequest;
import net.checkconsulting.assuranceiard.api.dto.RegenerationResponse;
import net.checkconsulting.assuranceiard.api.dto.SignatureContratRequest;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.securite.Acces;
import net.checkconsulting.assuranceiard.api.securite.TypeRessource;
import net.checkconsulting.assuranceiard.api.service.ContratService;
import net.checkconsulting.assuranceiard.api.service.EspaceClientService;
import net.checkconsulting.assuranceiard.api.service.document.RegenerationContratService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ContratResource {

    private final ContratService contratService;
    private final EspaceClientService espaceClientService;
    private final RegenerationContratService regenerationService;

    @PostMapping("/devis/contrat")
    @Acces(rubrique = Rubrique.SOUSCRIPTION, droit = Droit.CREER,
            ressource = TypeRessource.SOUSCRIPTION, identifiant = "demande.referenceDevis")
    @LogMetier(champs = {"demande.typeLogement", "demande.surface", "demande.franchise"})
    public ContratResponse generer(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody GenerationContratRequest demande) {
        return contratService.generer(authorization, demande);
    }

    /** US-04, AC1 / US-18 : les contrats habitation et auto du client connecté, et seulement les siens. */
    @GetMapping("/contrats")
    @Acces(rubrique = Rubrique.CONTRATS, droit = Droit.CONSULTER)
    @LogMetier(champs = {"resultat.taille"})
    public List<ContratResumeResponse> lister(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return espaceClientService.contrats(authorization);
    }

    /** US-18, AC3 : fiche d'un contrat par son numéro ; le contrat d'un autre client est refusé (AC7). */
    @GetMapping("/contrats/{numero}")
    @Acces(rubrique = Rubrique.CONTRATS, droit = Droit.CONSULTER,
            ressource = TypeRessource.CONTRAT_NUMERO, identifiant = "numero")
    @LogMetier(champs = {"resultat.produit", "resultat.statut"})
    public FicheContratResponse fiche(@PathVariable String numero) {
        return espaceClientService.fiche(numero);
    }

    /**
     * US-21, AC1 : régénère les conditions particulières (nouvelle version, PDF envoyé par e-mail). Droit
     * DOCUMENTS/CREER ; seul le propriétaire du contrat peut le faire, un refus est tracé dans acces_refuse (US-04).
     * 422 si le statut du contrat ne le permet pas (RG2, RG3 d'US-18), 429 si une régénération date de moins d'une
     * minute.
     */
    @PostMapping("/contrats/{numero}/conditions-particulieres")
    @Acces(rubrique = Rubrique.DOCUMENTS, droit = Droit.CREER,
            ressource = TypeRessource.CONTRAT_NUMERO, identifiant = "numero")
    @LogMetier(champs = {"resultat.version", "resultat.disponible"})
    public RegenerationResponse regenerer(@PathVariable String numero) {
        return regenerationService.regenerer(numero);
    }

    @GetMapping("/devis/contrat/{referenceDevis}")
    @Acces(rubrique = Rubrique.CONTRATS, droit = Droit.CONSULTER,
            ressource = TypeRessource.CONTRAT, identifiant = "referenceDevis")
    @LogMetier(champs = {"resultat.statut"})
    public ContratResponse obtenir(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID referenceDevis) {
        return contratService.obtenir(authorization, referenceDevis);
    }

    @PostMapping("/devis/contrat/{referenceDevis}/signature/code")
    @Acces(rubrique = Rubrique.SOUSCRIPTION, droit = Droit.CREER,
            ressource = TypeRessource.CONTRAT, identifiant = "referenceDevis")
    @LogMetier
    public EnvoyerCodeSignatureResponse envoyerCode(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID referenceDevis) {
        return contratService.envoyerCode(authorization, referenceDevis);
    }

    @PostMapping("/devis/contrat/{referenceDevis}/signature/code/renvoyer")
    @Acces(rubrique = Rubrique.SOUSCRIPTION, droit = Droit.CREER,
            ressource = TypeRessource.CONTRAT, identifiant = "referenceDevis")
    @LogMetier
    public EnvoyerCodeSignatureResponse renvoyerCode(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID referenceDevis) {
        return contratService.renvoyerCode(authorization, referenceDevis);
    }

    @PostMapping("/devis/contrat/{referenceDevis}/signature")
    @Acces(rubrique = Rubrique.SOUSCRIPTION, droit = Droit.CREER,
            ressource = TypeRessource.CONTRAT, identifiant = "referenceDevis")
    @LogMetier(champs = {"resultat.statut"})
    public ContratResponse signer(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID referenceDevis,
            @Valid @RequestBody SignatureContratRequest demande) {
        return contratService.signer(authorization, referenceDevis, demande);
    }
}
