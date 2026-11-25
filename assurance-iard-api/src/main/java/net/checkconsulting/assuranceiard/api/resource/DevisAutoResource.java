package net.checkconsulting.assuranceiard.api.resource;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.client.siv.SivClient;
import net.checkconsulting.assuranceiard.api.client.siv.SivReponse;
import net.checkconsulting.assuranceiard.api.dto.BesoinsAutoReponse;
import net.checkconsulting.assuranceiard.api.dto.BesoinsAutoRequest;
import net.checkconsulting.assuranceiard.api.dto.CriteresEligibiliteResponse;
import net.checkconsulting.assuranceiard.api.dto.DevisAutoCreationRequest;
import net.checkconsulting.assuranceiard.api.dto.DevisAutoCreationResponse;
import net.checkconsulting.assuranceiard.api.dto.FormuleAutoRequest;
import net.checkconsulting.assuranceiard.api.dto.FormuleAutoResponse;
import net.checkconsulting.assuranceiard.api.dto.MarqueResponse;
import net.checkconsulting.assuranceiard.api.dto.PrixFormuleAutoResponse;
import net.checkconsulting.assuranceiard.api.dto.SivRechercheRequest;
import net.checkconsulting.assuranceiard.api.dto.SivRechercheResponse;
import net.checkconsulting.assuranceiard.api.dto.TarifAutoRequest;
import net.checkconsulting.assuranceiard.api.dto.TarifAutoResponse;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.securite.AccesPublic;
import net.checkconsulting.assuranceiard.api.service.BesoinsAutoService;
import net.checkconsulting.assuranceiard.api.service.DevisAutoCreationService;
import net.checkconsulting.assuranceiard.api.service.DocumentService;
import net.checkconsulting.assuranceiard.api.service.EligibiliteAutoService;
import net.checkconsulting.assuranceiard.api.service.FormuleAutoService;
import net.checkconsulting.assuranceiard.api.service.ReferentielVehiculeService;
import net.checkconsulting.assuranceiard.api.service.TarificateurAutoService;
import net.checkconsulting.assuranceiard.api.service.document.DemandeDocument;
import net.checkconsulting.assuranceiard.api.service.document.DocumentsGeneresService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** US-27/28/29 : référentiels, recherche SIV, tarification et parcours devis auto. */
@RestController
@RequestMapping("/v1/devis/auto")
@RequiredArgsConstructor
public class DevisAutoResource {

    private final SivClient sivClient;
    private final ReferentielVehiculeService referentielVehiculeService;
    private final EligibiliteAutoService eligibiliteAutoService;
    private final TarificateurAutoService tarificateurAutoService;
    private final DevisAutoCreationService devisAutoCreationService;
    private final BesoinsAutoService besoinsAutoService;
    private final FormuleAutoService formuleAutoService;
    private final DocumentsGeneresService documentsGeneresService;

    @AccesPublic
    @GetMapping("/referentiels/vehicules")
    public List<MarqueResponse> vehicules() {
        return referentielVehiculeService.parMarque();
    }

    @AccesPublic
    @GetMapping("/referentiels/criteres-eligibilite")
    public CriteresEligibiliteResponse criteresEligibilite() {
        return eligibiliteAutoService.criteres();
    }

    @AccesPublic
    @PostMapping("/recherche-vehicule")
    public SivRechercheResponse rechercherVehicule(@Valid @RequestBody SivRechercheRequest demande) {
        String plaque = normaliserPlaque(demande.immatriculation());
        SivReponse reponse = sivClient.consulterPlaque(plaque);
        if (reponse.vehicule() != null) {
            var v = reponse.vehicule();
            return new SivRechercheResponse(reponse.statut(), plaque,
                    v.refVehiculeId(), v.marque(), v.modele(), v.energie(),
                    v.version(), v.annee(), v.valeurNeuve());
        }
        return new SivRechercheResponse(reponse.statut(), plaque,
                null, null, null, null, null, null, null);
    }

    /** US-28 : estimation du tarif auto (étape 1.4, encart en temps réel). */
    @AccesPublic
    @PostMapping("/tarif")
    public TarifAutoResponse estimerTarif(@Valid @RequestBody TarifAutoRequest demande) {
        return tarificateurAutoService.estimer(demande);
    }

    /** US-28 / US-59 : crée le devis auto après validation du profil conducteur. */
    @AccesPublic
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public DevisAutoCreationResponse creerDevis(@Valid @RequestBody DevisAutoCreationRequest demande) {
        return devisAutoCreationService.creer(demande);
    }

    /** US-29 : enregistre les besoins et retourne la formule conseillée (RG1). */
    @AccesPublic
    @PostMapping("/{id}/besoins")
    public BesoinsAutoReponse enregistrerBesoins(
            @PathVariable Long id,
            @Valid @RequestBody BesoinsAutoRequest demande) {
        return besoinsAutoService.enregistrerBesoins(id, demande);
    }

    /** US-29 étape 3 : prix des 3 formules et des options pour ce devis. */
    @AccesPublic
    @GetMapping("/{id}/prix-formules")
    public PrixFormuleAutoResponse obtenirPrixFormules(@PathVariable Long id) {
        return formuleAutoService.obtenirPrix(id);
    }

    /** US-29 étape 3 : enregistre la formule et les options choisies. */
    @AccesPublic
    @PutMapping("/{id}/formule")
    public FormuleAutoResponse choisirFormule(
            @PathVariable Long id,
            @Valid @RequestBody FormuleAutoRequest demande) {
        return formuleAutoService.choisirFormule(id, demande);
    }

    /** US-29 étape 3 : génère et télécharge la fiche conseil (PDF). */
    @AccesPublic
    @GetMapping("/{id}/fiche-conseil")
    public ResponseEntity<byte[]> ficheConseil(@PathVariable Long id) {
        Document doc = documentsGeneresService.generer(
                DemandeDocument.commun(TypeDocument.FICHE_CONSEIL, String.valueOf(id), "DEVIS"));
        DocumentService.Fichier fichier = documentsGeneresService.telecharger(doc.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fichier.nom(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(fichier.contenu());
    }

    static String normaliserPlaque(String input) {
        if (input == null || input.isBlank()) return "";
        String stripped = input.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (stripped.length() == 7
                && stripped.substring(0, 2).chars().allMatch(Character::isLetter)
                && stripped.substring(2, 5).chars().allMatch(Character::isDigit)
                && stripped.substring(5).chars().allMatch(Character::isLetter)) {
            return stripped.substring(0, 2) + "-" + stripped.substring(2, 5) + "-" + stripped.substring(5);
        }
        return input.toUpperCase().trim();
    }
}
