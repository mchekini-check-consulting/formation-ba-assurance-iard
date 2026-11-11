package net.checkconsulting.assuranceiard.api.service;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.HistoriqueEntreeDto;
import net.checkconsulting.assuranceiard.api.dto.MiseAJourPermissionsRequest;
import net.checkconsulting.assuranceiard.api.dto.PermissionDto;
import net.checkconsulting.assuranceiard.api.dto.ProfilInfoDto;
import net.checkconsulting.assuranceiard.api.exception.AccesNonAutoriseException;
import net.checkconsulting.assuranceiard.api.exception.InformationRefuseeException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.HistoriquePermission;
import net.checkconsulting.assuranceiard.api.model.Permission;
import net.checkconsulting.assuranceiard.api.model.Profil;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.repository.HistoriquePermissionRepository;
import net.checkconsulting.assuranceiard.api.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfilService {

    private static final Map<Profil, String> DESCRIPTIONS = Map.of(
            Profil.PROSPECT, "Utilisateur non encore assuré, en cours de souscription.",
            Profil.ASSURE, "Client disposant d'un contrat actif.",
            Profil.GESTIONNAIRE, "Collaborateur interne gérant les dossiers.",
            Profil.ADMINISTRATEUR, "Administrateur disposant de tous les droits."
    );

    private final PermissionRepository permissionRepository;
    private final HistoriquePermissionRepository historiqueRepository;
    private final CompteRepository compteRepository;
    private final Clock horloge;

    public List<ProfilInfoDto> listerProfils() {
        return Arrays.stream(Profil.values())
                .map(this::construireProfilInfo)
                .toList();
    }

    private ProfilInfoDto construireProfilInfo(Profil profil) {
        long nombre = compteRepository.countByProfil(profil);
        List<PermissionDto> permissions = permissionRepository.findByProfil(profil).stream()
                .map(p -> new PermissionDto(p.getRubrique().name(), p.getDroit().name(), p.isValeur()))
                .toList();
        return new ProfilInfoDto(profil.name(), DESCRIPTIONS.get(profil), nombre, permissions);
    }

    @Transactional
    public void mettreAJourPermissions(Profil profil, MiseAJourPermissionsRequest demande, Compte administrateur) {
        validerDroitsAdministrateur(profil, demande);

        List<Permission> existantes = permissionRepository.findByProfil(profil);
        Map<String, Permission> index = existantes.stream()
                .collect(Collectors.toMap(p -> cle(p.getRubrique(), p.getDroit()), p -> p));

        for (PermissionDto dto : demande.permissions()) {
            Rubrique rubrique = Rubrique.valueOf(dto.rubrique());
            Droit droit = Droit.valueOf(dto.droit());
            Permission permission = index.get(cle(rubrique, droit));
            if (permission != null && permission.isValeur() != dto.valeur()) {
                HistoriquePermission historique = new HistoriquePermission();
                historique.setModifieLe(horloge.instant());
                historique.setModifieParEmail(administrateur.getEmail());
                historique.setProfil(profil);
                historique.setRubrique(rubrique);
                historique.setDroit(droit);
                historique.setAncienneValeur(permission.isValeur());
                historique.setNouvelleValeur(dto.valeur());
                historiqueRepository.save(historique);
                permission.setValeur(dto.valeur());
            }
        }
    }

    public List<HistoriqueEntreeDto> lireHistorique(Profil profil) {
        return historiqueRepository.findByProfilOrderByModifieLeDesc(profil).stream()
                .map(h -> new HistoriqueEntreeDto(
                        h.getModifieLe(),
                        h.getRubrique().name(),
                        h.getDroit().name(),
                        h.isAncienneValeur(),
                        h.isNouvelleValeur()))
                .toList();
    }

    private void validerDroitsAdministrateur(Profil profil, MiseAJourPermissionsRequest demande) {
        if (profil == Profil.ADMINISTRATEUR) {
            boolean tentativeSuppressionProfils = demande.permissions().stream()
                    .anyMatch(p -> Rubrique.PROFILS.name().equals(p.rubrique()) && !p.valeur());
            if (tentativeSuppressionProfils) {
                throw new InformationRefuseeException("permissions",
                        "Les droits sur la rubrique Profils ne peuvent pas être retirés à l'Administrateur.");
            }
        }
    }

    private String cle(Rubrique rubrique, Droit droit) {
        return rubrique.name() + "_" + droit.name();
    }

    public void verifierAdministrateur(Compte compte) {
        if (compte.getProfil() != Profil.ADMINISTRATEUR) {
            throw new AccesNonAutoriseException();
        }
    }
}
