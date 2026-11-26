package net.checkconsulting.assuranceiard.api.resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.AcceptationAvenantRequest;
import net.checkconsulting.assuranceiard.api.dto.AvenantAccepteResponse;
import net.checkconsulting.assuranceiard.api.dto.AvenantResponse;
import net.checkconsulting.assuranceiard.api.dto.GarantiesContratResponse;
import net.checkconsulting.assuranceiard.api.dto.PreparationAvenantRequest;
import net.checkconsulting.assuranceiard.api.dto.ProjetAvenantResponse;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.securite.Acces;
import net.checkconsulting.assuranceiard.api.securite.TypeRessource;
import net.checkconsulting.assuranceiard.api.service.avenant.AcceptationAvenantService;
import net.checkconsulting.assuranceiard.api.service.avenant.AvenantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-23 : modifier ses garanties et accepter l'avenant. Seul le propriétaire du contrat y a accès (US-04, refus tracé
 * dans acces_refuse). Codes : 422 si le contrat n'est pas actif (AC6), 400 si la demande est invalide (aucune
 * garantie changée, garantie obligatoire retirée, case d'acceptation non cochée), 404 si aucun avenant n'est en
 * attente, 503 si le moteur tarifaire ne répond pas.
 */
@RestController
@RequiredArgsConstructor
public class AvenantResource {

    private static final String RELAIS = "X-Forwarded-For";
    private static final int TAILLE_IP = 45;

    private final AvenantService avenants;
    private final AcceptationAvenantService acceptations;

    /** AC1, AC2, AC5 : garanties actuelles et ajoutables, avec leur prix ; avenant en attente s'il y en a un. */
    @GetMapping("/contrats/{numero}/garanties")
    @Acces(rubrique = Rubrique.CONTRATS, droit = Droit.CONSULTER,
            ressource = TypeRessource.CONTRAT_NUMERO, identifiant = "numero")
    @LogMetier(champs = {"resultat.produit", "resultat.statut"})
    public GarantiesContratResponse garanties(@PathVariable String numero) {
        return avenants.garanties(numero);
    }

    /** AC3 : prépare l'avenant (garanties voulues après l'avenant) ; remplace un avenant déjà en attente. */
    @PostMapping("/contrats/{numero}/avenants")
    @Acces(rubrique = Rubrique.CONTRATS, droit = Droit.MODIFIER,
            ressource = TypeRessource.CONTRAT_NUMERO, identifiant = "numero")
    @LogMetier(champs = {"resultat.reductionCouverture"})
    public ProjetAvenantResponse preparer(@PathVariable String numero,
            @Valid @RequestBody PreparationAvenantRequest demande) {
        return avenants.preparer(numero, demande);
    }

    /** AC4, AC7 : accepte l'avenant en attente (case cochée), horodaté et conservé. */
    @PostMapping("/contrats/{numero}/avenants/en-attente/acceptation")
    @Acces(rubrique = Rubrique.CONTRATS, droit = Droit.MODIFIER,
            ressource = TypeRessource.CONTRAT_NUMERO, identifiant = "numero")
    @LogMetier(champs = {"resultat.disponible", "resultat.versionConditionsParticulieres"})
    public AvenantAccepteResponse accepter(@PathVariable String numero,
            @RequestBody AcceptationAvenantRequest demande, HttpServletRequest requete) {
        return acceptations.accepter(numero, demande, adresseIp(requete));
    }

    /** AC5 : abandonne l'avenant en attente (statut ABANDONNE, rien n'est effacé). */
    @PostMapping("/contrats/{numero}/avenants/en-attente/abandon")
    @Acces(rubrique = Rubrique.CONTRATS, droit = Droit.MODIFIER,
            ressource = TypeRessource.CONTRAT_NUMERO, identifiant = "numero")
    @LogMetier(champs = {"resultat.statut"})
    public AvenantResponse abandonner(@PathVariable String numero) {
        return avenants.abandonner(numero);
    }

    /** Adresse du client (première adresse relayée par le gateway, sinon celle de la connexion). */
    static String adresseIp(HttpServletRequest requete) {
        String relayee = requete.getHeader(RELAIS);
        String adresse = relayee == null || relayee.isBlank() ? requete.getRemoteAddr() : relayee.split(",")[0].trim();
        return adresse == null || adresse.length() <= TAILLE_IP ? adresse : adresse.substring(0, TAILLE_IP);
    }
}
