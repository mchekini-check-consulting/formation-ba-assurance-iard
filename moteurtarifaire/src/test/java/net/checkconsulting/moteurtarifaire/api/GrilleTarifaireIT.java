package net.checkconsulting.moteurtarifaire.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import net.checkconsulting.moteurtarifaire.api.model.ParametreTarifaire;
import net.checkconsulting.moteurtarifaire.api.model.VersionGrille;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire;
import net.checkconsulting.moteurtarifaire.api.service.GrilleTarifaireService;
import net.checkconsulting.moteurtarifaire.api.repository.ParametreTarifaireRepository;
import net.checkconsulting.moteurtarifaire.api.repository.VersionGrilleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class GrilleTarifaireIT {

    private static final LocalDate DERNIER_JOUR = LocalDate.of(2030, 12, 31);
    private static final String NOUVELLE_VERSION = "nouvelle-version";

    @Autowired
    private GrilleTarifaireService grilles;

    @Autowired
    private VersionGrilleRepository versions;

    @Autowired
    private ParametreTarifaireRepository parametres;

    @BeforeEach
    void installerLaGrille() {
        JeuxDeDonnees.installerGrilleDeReference(versions, parametres);
    }

    private void changerDeGrilleApres(LocalDate dernierJour) {
        VersionGrille reference = versions.findAll().getFirst();
        reference.setDateFin(dernierJour);
        VersionGrille nouvelle = new VersionGrille();
        nouvelle.setVersion(NOUVELLE_VERSION);
        nouvelle.setDateDebut(dernierJour.plusDays(1));
        Long idNouvelle = versions.save(nouvelle).getId();
        List<ParametreTarifaire> copies = parametres.findByGrilleId(reference.getId()).stream()
                .map(parametre -> {
                    ParametreTarifaire copie = JeuxDeDonnees.parametre(
                            parametre.getCategorie(), parametre.getCle(), parametre.getValeur());
                    copie.setGrilleId(idNouvelle);
                    return copie;
                })
                .toList();
        parametres.saveAll(copies);
    }

    @Test
    void lesDatesDeDebutEtDeFinSontIncluses() {
        changerDeGrilleApres(DERNIER_JOUR);

        assertThat(grilles.applicableAu(DERNIER_JOUR)).map(GrilleTarifaire::version)
                .contains(JeuxDeDonnees.GRILLE.version());
        assertThat(grilles.applicableAu(DERNIER_JOUR.plusDays(1))).map(GrilleTarifaire::version)
                .contains(NOUVELLE_VERSION);
    }

    @Test
    void uneNouvelleGrilleEstPriseEnCompteSansRedemarrage() {
        LocalDate hier = LocalDate.now().minusDays(1);

        changerDeGrilleApres(hier);

        assertThat(grilles.applicableAu(LocalDate.now())).map(GrilleTarifaire::version).contains(NOUVELLE_VERSION);
    }

    @Test
    void avantLaPremiereDateAucuneGrilleNestEnVigueur() {
        LocalDate debut = versions.findAll().getFirst().getDateDebut();

        assertThat(grilles.applicableAu(debut.minusDays(1))).isEmpty();
    }

    @Test
    void laBaseRefuseUneDateDeFinAvantLaDateDeDebut() {
        VersionGrille version = new VersionGrille();
        version.setVersion(NOUVELLE_VERSION);
        version.setDateDebut(DERNIER_JOUR);
        version.setDateFin(DERNIER_JOUR.minusDays(1));

        assertThatThrownBy(() -> versions.saveAndFlush(version)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseRefuseDeuxFoisLeMemeParametreDansUneVersion() {
        ParametreTarifaire existant = parametres.findAll().getFirst();
        ParametreTarifaire doublon = JeuxDeDonnees.parametre(existant.getCategorie(), existant.getCle(), existant.getValeur());
        doublon.setGrilleId(existant.getGrilleId());

        assertThatThrownBy(() -> parametres.saveAndFlush(doublon)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
