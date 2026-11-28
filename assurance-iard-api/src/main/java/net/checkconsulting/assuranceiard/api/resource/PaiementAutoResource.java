package net.checkconsulting.assuranceiard.api.resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.ContratAutoGenerationResponse;
import net.checkconsulting.assuranceiard.api.dto.ContratAutoResponse;
import net.checkconsulting.assuranceiard.api.dto.EnvoyerCodeSignatureResponse;
import net.checkconsulting.assuranceiard.api.dto.IbanVerificationRequest;
import net.checkconsulting.assuranceiard.api.dto.IbanVerificationResponse;
import net.checkconsulting.assuranceiard.api.dto.PaiementAutoInitResponse;
import net.checkconsulting.assuranceiard.api.dto.SignatureContratRequest;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.securite.Acces;
import net.checkconsulting.assuranceiard.api.service.PaiementAutoService;
import net.checkconsulting.assuranceiard.api.service.SignatureAutoService;
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

/** US-31 / US-33 : paiement et signature du contrat auto. */
@RestController
@RequestMapping("/v1/devis/auto")
@RequiredArgsConstructor
public class PaiementAutoResource {

    private final PaiementAutoService paiementAutoService;
    private final SignatureAutoService signatureAutoService;

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

    /** US-33 B1 : charge les données du contrat pour l'écran de signature (GET étape 6). */
    @GetMapping("/{id}/contrat/signature")
    @Acces(rubrique = Rubrique.SOUSCRIPTION, droit = Droit.CONSULTER)
    public ContratAutoResponse obtenirContrat(
            @PathVariable Long id,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return signatureAutoService.obtenir(id, authorization);
    }

    /** US-33 B2 : envoie le code de signature par e-mail (première fois). */
    @PostMapping("/{id}/contrat/signature/code")
    @Acces(rubrique = Rubrique.SOUSCRIPTION, droit = Droit.CREER)
    public EnvoyerCodeSignatureResponse envoyerCode(
            @PathVariable Long id,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return signatureAutoService.envoyerCode(id, authorization);
    }

    /** US-33 B3 : renvoie un nouveau code de signature (invalide le précédent). */
    @PostMapping("/{id}/contrat/signature/code/renvoyer")
    @Acces(rubrique = Rubrique.SOUSCRIPTION, droit = Droit.CREER)
    public EnvoyerCodeSignatureResponse renvoyerCode(
            @PathVariable Long id,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return signatureAutoService.renvoyerCode(id, authorization);
    }

    /** US-33 B4 : signe le contrat après vérification du code. */
    @PostMapping("/{id}/contrat/signature")
    @Acces(rubrique = Rubrique.SOUSCRIPTION, droit = Droit.CREER)
    public ContratAutoResponse signerContrat(
            @PathVariable Long id,
            @Valid @RequestBody SignatureContratRequest demande,
            HttpServletRequest requete,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String adresseIp = requete.getRemoteAddr();
        return signatureAutoService.signer(id, demande, adresseIp, authorization);
    }
}
