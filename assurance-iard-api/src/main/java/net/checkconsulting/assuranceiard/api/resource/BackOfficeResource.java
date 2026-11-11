package net.checkconsulting.assuranceiard.api.resource;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.AccesRefuseResponse;
import net.checkconsulting.assuranceiard.api.dto.ClientResumeResponse;
import net.checkconsulting.assuranceiard.api.dto.DossierClientResponse;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.securite.Acces;
import net.checkconsulting.assuranceiard.api.securite.JournalAccesRefuses;
import net.checkconsulting.assuranceiard.api.service.BackOfficeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** US-04 : back-office, réservé aux profils internes (Gestionnaire, Administrateur). */
@RestController
@RequiredArgsConstructor
public class BackOfficeResource {

    private final BackOfficeService backOfficeService;
    private final JournalAccesRefuses journal;

    @GetMapping("/back-office/clients")
    @Acces(rubrique = Rubrique.COMPTES_UTILISATEURS, droit = Droit.CONSULTER, backOffice = true)
    @LogMetier(champs = {"resultat.taille"})
    public List<ClientResumeResponse> listerClients() {
        return backOfficeService.listerClients();
    }

    @GetMapping("/back-office/clients/{id}")
    @Acces(rubrique = Rubrique.COMPTES_UTILISATEURS, droit = Droit.CONSULTER, backOffice = true)
    @LogMetier
    public DossierClientResponse dossierClient(@PathVariable Long id) {
        return backOfficeService.dossier(id);
    }

    /** RG3 : journal des accès refusés, pour l'administrateur (seul profil avec « Profils / Consulter »). */
    @GetMapping("/admin/acces-refuses")
    @Acces(rubrique = Rubrique.PROFILS, droit = Droit.CONSULTER, backOffice = true)
    @LogMetier(champs = {"resultat.taille"})
    public List<AccesRefuseResponse> accesRefuses() {
        return journal.derniers();
    }
}
