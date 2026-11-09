package net.checkconsulting.moteurtarifaire.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleInvalideException;
import net.checkconsulting.moteurtarifaire.api.mapper.GrilleTarifaireAssembleur;
import net.checkconsulting.moteurtarifaire.api.model.VersionGrille;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire;
import net.checkconsulting.moteurtarifaire.api.repository.ParametreTarifaireRepository;
import net.checkconsulting.moteurtarifaire.api.repository.VersionGrilleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GrilleTarifaireServiceTest {

    private static final LocalDate DATE = LocalDate.of(2030, 6, 15);

    @Mock
    private VersionGrilleRepository versions;

    @Mock
    private ParametreTarifaireRepository parametres;

    private GrilleTarifaireService service;

    @BeforeEach
    void preparer() {
        service = new GrilleTarifaireService(
                versions, parametres, new GrilleTarifaireAssembleur(), JeuxDeDonnees.validateurDeGrille());
        lenient().when(parametres.findByGrilleId(any())).thenAnswer(appel -> JeuxDeDonnees.parametresDeReference());
    }

    private VersionGrille version(String nom, LocalDate debut, LocalDate fin) {
        VersionGrille version = new VersionGrille();
        version.setVersion(nom);
        version.setDateDebut(debut);
        version.setDateFin(fin);
        return version;
    }

    @Test
    void renvoieLaGrilleEnVigueur() {
        when(versions.findApplicablesAu(DATE)).thenReturn(List.of(version("a", DATE, null)));

        assertThat(service.applicableAu(DATE)).map(GrilleTarifaire::version).contains("a");
    }

    @Test
    void sansGrilleEnVigueurLeResultatEstVide() {
        when(versions.findApplicablesAu(DATE)).thenReturn(List.of());

        assertThat(service.applicableAu(DATE)).isEmpty();
    }

    @Test
    void deuxGrillesEnVigueurLeMemeJourSontUneErreur() {
        when(versions.findApplicablesAu(DATE)).thenReturn(List.of(version("a", DATE, null), version("b", DATE, null)));

        assertThatThrownBy(() -> service.applicableAu(DATE))
                .isInstanceOf(GrilleInvalideException.class)
                .hasMessageContaining("Plusieurs");
    }

    @Test
    void desPeriodesQuiSeSuiventSontAcceptees() {
        when(versions.findAllByOrderByDateDebutAsc()).thenReturn(List.of(
                version("a", DATE, DATE.plusDays(9)),
                version("b", DATE.plusDays(10), null)));

        assertThatCode(service::verifierToutes).doesNotThrowAnyException();
    }

    @Test
    void uneVersionQuiCommenceLeDernierJourDeLaPrecedenteEstRefusee() {
        when(versions.findAllByOrderByDateDebutAsc()).thenReturn(List.of(
                version("a", DATE, DATE.plusDays(9)),
                version("b", DATE.plusDays(9), null)));

        assertThatThrownBy(service::verifierToutes)
                .isInstanceOf(GrilleInvalideException.class)
                .hasMessageContaining("chevauchent");
    }

    @Test
    void uneVersionSansFinSuivieDUneAutreEstRefusee() {
        when(versions.findAllByOrderByDateDebutAsc()).thenReturn(List.of(
                version("a", DATE, null),
                version("b", DATE.plusDays(10), null)));

        assertThatThrownBy(service::verifierToutes).isInstanceOf(GrilleInvalideException.class);
    }

    @Test
    void sansAucuneGrilleLeControleEchoue() {
        when(versions.findAllByOrderByDateDebutAsc()).thenReturn(List.of());

        assertThatThrownBy(service::verifierToutes).isInstanceOf(GrilleInvalideException.class);
    }
}
