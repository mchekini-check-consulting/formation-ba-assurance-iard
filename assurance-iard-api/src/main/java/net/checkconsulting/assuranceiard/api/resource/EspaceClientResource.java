package net.checkconsulting.assuranceiard.api.resource;

import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.MesFacturesResponse;
import net.checkconsulting.assuranceiard.api.dto.TableauDeBordResponse;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.securite.Acces;
import net.checkconsulting.assuranceiard.api.service.EspaceClientService;
import net.checkconsulting.assuranceiard.api.service.facture.FactureService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-18 : tableau de bord du client connecté, construit uniquement à partir de ses propres données (AC7).
 * Droit CONTRATS/CONSULTER, accordé au Prospect depuis US-04 (V18) : un prospect sans contrat y accède (AC6).
 * Les devis et les sinistres ne sont remplis que si le profil a aussi le droit de les consulter.
 */
@RestController
@RequiredArgsConstructor
public class EspaceClientResource {

    private final EspaceClientService service;
    private final FactureService factures;

    @GetMapping("/espace-client/tableau-de-bord")
    @Acces(rubrique = Rubrique.CONTRATS, droit = Droit.CONSULTER)
    @LogMetier
    public TableauDeBordResponse tableauDeBord(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return service.tableauDeBord(authorization);
    }

    /**
     * US-22, AC1, AC2, AC4 : « Mes factures », échéancier et factures de chaque contrat signé du client connecté, et
     * seulement les siens. Droit CONTRATS/CONSULTER.
     */
    @GetMapping("/espace-client/factures")
    @Acces(rubrique = Rubrique.CONTRATS, droit = Droit.CONSULTER)
    @LogMetier(champs = {"resultat.aucuneCotisation"})
    public MesFacturesResponse mesFactures(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return factures.mesFactures(authorization);
    }
}