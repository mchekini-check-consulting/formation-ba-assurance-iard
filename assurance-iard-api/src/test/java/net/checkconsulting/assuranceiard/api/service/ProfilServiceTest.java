package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfilServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-04T10:00:00Z");

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private HistoriquePermissionRepository historiqueRepository;

    @Mock
    private CompteRepository compteRepository;

    private ProfilService profilService;
    private Compte administrateur;

    @BeforeEach
    void preparer() {
        profilService = new ProfilService(permissionRepository, historiqueRepository, compteRepository,
                Clock.fixed(MAINTENANT, ZoneOffset.UTC));
        administrateur = new Compte();
        administrateur.setEmail("admin@exemple.fr");
        administrateur.setProfil(Profil.ADMINISTRATEUR);
    }

    @Test
    void listerRetourneQuatreProfils() {
        when(compteRepository.countByProfil(any())).thenReturn(0L);
        when(permissionRepository.findByProfil(any())).thenReturn(List.of());

        List<ProfilInfoDto> profils = profilService.listerProfils();

        assertThat(profils).hasSize(4);
        assertThat(profils).extracting(ProfilInfoDto::nom)
                .containsExactly("PROSPECT", "ASSURE", "GESTIONNAIRE", "ADMINISTRATEUR");
    }

    @Test
    void miseAJourEnregistreLHistoriqueQuandLaValeurChange() {
        Permission permission = new Permission();
        permission.setProfil(Profil.ASSURE);
        permission.setRubrique(Rubrique.DEVIS);
        permission.setDroit(Droit.CREER);
        permission.setValeur(true);
        when(permissionRepository.findByProfil(Profil.ASSURE)).thenReturn(List.of(permission));

        profilService.mettreAJourPermissions(Profil.ASSURE,
                new MiseAJourPermissionsRequest(List.of(new PermissionDto("DEVIS", "CREER", false))),
                administrateur);

        ArgumentCaptor<HistoriquePermission> captor = ArgumentCaptor.forClass(HistoriquePermission.class);
        verify(historiqueRepository).save(captor.capture());
        assertThat(captor.getValue().isAncienneValeur()).isTrue();
        assertThat(captor.getValue().isNouvelleValeur()).isFalse();
        assertThat(captor.getValue().getModifieParEmail()).isEqualTo("admin@exemple.fr");
        assertThat(captor.getValue().getModifieLe()).isEqualTo(MAINTENANT);
        assertThat(permission.isValeur()).isFalse();
    }

    @Test
    void miseAJourNEnregistrePasHistoriqueQuandValeurIdentique() {
        Permission permission = new Permission();
        permission.setProfil(Profil.ASSURE);
        permission.setRubrique(Rubrique.DEVIS);
        permission.setDroit(Droit.CREER);
        permission.setValeur(true);
        when(permissionRepository.findByProfil(Profil.ASSURE)).thenReturn(List.of(permission));

        profilService.mettreAJourPermissions(Profil.ASSURE,
                new MiseAJourPermissionsRequest(List.of(new PermissionDto("DEVIS", "CREER", true))),
                administrateur);

        verify(historiqueRepository, never()).save(any());
    }

    @Test
    void refuseSuppressionDroitsProfilsAdministrateur() {
        assertThatThrownBy(() -> profilService.mettreAJourPermissions(Profil.ADMINISTRATEUR,
                new MiseAJourPermissionsRequest(List.of(new PermissionDto("PROFILS", "CONSULTER", false))),
                administrateur))
                .isInstanceOf(InformationRefuseeException.class);
    }

    @Test
    void refuseUnCompteNonAdministrateur() {
        Compte gestionnaire = new Compte();
        gestionnaire.setProfil(Profil.GESTIONNAIRE);

        assertThatThrownBy(() -> profilService.verifierAdministrateur(gestionnaire))
                .isInstanceOf(AccesNonAutoriseException.class);
    }

    @Test
    void accepteUnAdministrateur() {
        profilService.verifierAdministrateur(administrateur);
        // aucune exception attendue
    }
}
