package net.checkconsulting.assuranceiard.api.resource;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.HistoriqueEntreeDto;
import net.checkconsulting.assuranceiard.api.dto.MiseAJourPermissionsRequest;
import net.checkconsulting.assuranceiard.api.dto.ProfilInfoDto;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Profil;
import net.checkconsulting.assuranceiard.api.service.ProfilService;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/profils")
@RequiredArgsConstructor
public class ProfilResource {

    private final ProfilService profilService;
    private final SessionService sessionService;

    @GetMapping
    @LogMetier(champs = {"resultat.taille"})
    public List<ProfilInfoDto> lister(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        profilService.verifierAdministrateur(compte);
        return profilService.listerProfils();
    }

    @PutMapping("/{profil}/permissions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @LogMetier(champs = {"profil"})
    public void mettreAJourPermissions(
            @PathVariable String profil,
            @RequestBody MiseAJourPermissionsRequest demande,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        profilService.verifierAdministrateur(compte);
        profilService.mettreAJourPermissions(Profil.valueOf(profil), demande, compte);
    }

    @GetMapping("/{profil}/historique")
    @LogMetier(champs = {"profil", "resultat.taille"})
    public List<HistoriqueEntreeDto> lireHistorique(
            @PathVariable String profil,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        profilService.verifierAdministrateur(compte);
        return profilService.lireHistorique(Profil.valueOf(profil));
    }
}
