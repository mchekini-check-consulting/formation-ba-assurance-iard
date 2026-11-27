package net.checkconsulting.assuranceiard.api.resource;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.ContratAutoGenerationResponse;
import net.checkconsulting.assuranceiard.api.dto.IbanVerificationRequest;
import net.checkconsulting.assuranceiard.api.dto.IbanVerificationResponse;
import net.checkconsulting.assuranceiard.api.dto.PaiementAutoInitResponse;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.securite.Acces;
import net.checkconsulting.assuranceiard.api.service.PaiementAutoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** US-31 étape 5 : initialisation de l'écran paiement, vérification IBAN et génération du contrat auto. */
@RestController
@RequestMapping("/v1/devis/auto")
@RequiredArgsConstructor
public class PaiementAutoResource {

    private final PaiementAutoService paiementAutoService;

    /** B1 : charge les données initiales de l'étape 5 (récapitulatif, loi Hamon, contrat existant). */
    @GetMapping("/{id}/paiement")
    @Acces(rubrique = Rubrique.SOUSCRIPTION, droit = Droit.CONSULTER)
    public PaiementAutoInitResponse initialiser(
            @PathVariable Long id,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return paiementAutoService.initialiser(id, authorization);
    }

    /** B4 : vérifie l'IBAN saisi (bouchon, RG4). */
    @PostMapping("/iban/verifier")
    @Acces(rubrique = Rubrique.SOUSCRIPTION, droit = Droit.CONSULTER)
    public IbanVerificationResponse verifierIban(@Valid @RequestBody IbanVerificationRequest demande) {
        return paiementAutoService.verifierIban(demande);
    }

    /**
     * B7 : génère le contrat auto en une transaction (mandat SEPA, contrat, échéancier, PDF).
     * B8 : envoie l'e-mail avec le mandat en PJ après la transaction (RG6 : sans l'IBAN).
     */
    @PostMapping("/{id}/contrat")
    @Acces(rubrique = Rubrique.SOUSCRIPTION, droit = Droit.CREER)
    @ResponseStatus(HttpStatus.CREATED)
    public ContratAutoGenerationResponse genererContrat(
            @PathVariable Long id,
            @Valid @RequestBody net.checkconsulting.assuranceiard.api.dto.PaiementAutoRequest demande,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        ContratAutoGenerationResponse resultat = paiementAutoService.genererContrat(id, demande, authorization);
        if (resultat.documentMandatSepaId() != null) {
            paiementAutoService.envoyerEmailMandatSepa(
                    resultat.documentMandatSepaId(),
                    resultat.email(),
                    resultat.prenom(),
                    resultat.nom(),
                    resultat.numero(),
                    resultat.premierPrelevement(),
                    resultat.montantMensuel());
        }
        return resultat;
    }
}
