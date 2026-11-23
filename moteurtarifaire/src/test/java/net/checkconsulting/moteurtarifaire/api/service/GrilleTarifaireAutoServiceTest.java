package net.checkconsulting.moteurtarifaire.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaireAuto;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleInvalideException;
import net.checkconsulting.moteurtarifaire.api.mapper.GrilleTarifaireAutoAssembleur;
import net.checkconsulting.moteurtarifaire.api.model.RefFormule;
import net.checkconsulting.moteurtarifaire.api.model.RefOption;
import net.checkconsulting.moteurtarifaire.api.model.RefTarif;
import net.checkconsulting.moteurtarifaire.api.model.VersionGrille;
import net.checkconsulting.moteurtarifaire.api.repository.RefFormuleRepository;
import net.checkconsulting.moteurtarifaire.api.repository.RefOptionRepository;
import net.checkconsulting.moteurtarifaire.api.repository.RefTarifRepository;
import net.checkconsulting.moteurtarifaire.api.repository.VersionGrilleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GrilleTarifaireAutoServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 6, 15);

    @Mock
    private VersionGrilleRepository versions;

    @Mock
    private RefFormuleRepository formules;

    @Mock
    private RefOptionRepository options;

    @Mock
    private RefTarifRepository tarifs;

    private GrilleTarifaireAutoService service;

    @BeforeEach
    void preparer() {
        service = new GrilleTarifaireAutoService(versions, formules, options, tarifs,
                new GrilleTarifaireAutoAssembleur());
        lenient().when(formules.findByGrilleIdOrderByOrdre(any())).thenReturn(uneFormule());
        lenient().when(options.findByGrilleIdAndProduitOrderByCode(any(), eq("AUTO"))).thenReturn(List.of());
        lenient().when(tarifs.findByGrilleId(any())).thenReturn(tarifsComplets());
    }

    private VersionGrille version(String nom, LocalDate debut, LocalDate fin) {
        VersionGrille v = new VersionGrille();
        v.setId(1L);
        v.setProduit("AUTO");
        v.setVersion(nom);
        v.setDateDebut(debut);
        v.setDateFin(fin);
        return v;
    }

    private List<RefFormule> uneFormule() {
        RefFormule f = new RefFormule();
        f.setCode("TIERS");
        f.setLibelle("Tiers");
        f.setPrixMensuelBase(new BigDecimal("25.00"));
        f.setOrdre(1);
        return List.of(f);
    }

    private List<RefTarif> tarifsComplets() {
        String[] criteres = {"AGE", "BONUS_MALUS", "USAGE", "VALEUR_VEHICULE", "KM_ANNUEL", "STATIONNEMENT", "SURPRIME_NOVICE"};
        return java.util.Arrays.stream(criteres).map(c -> {
            RefTarif t = new RefTarif();
            t.setGrilleId(1L);
            t.setCritere(c);
            t.setValeur("OUI");
            t.setCoefficient(BigDecimal.ONE);
            return t;
        }).toList();
    }

    @Test
    void renvoieLaGrilleEnVigueur() {
        when(versions.findApplicablesAu(DATE, "AUTO")).thenReturn(List.of(version("AUTO-2026.1", DATE, null)));

        assertThat(service.applicableAu(DATE))
                .map(GrilleTarifaireAuto::version)
                .contains("AUTO-2026.1");
    }

    @Test
    void sansGrilleEnVigueurLeResultatEstVide() {
        when(versions.findApplicablesAu(DATE, "AUTO")).thenReturn(List.of());

        assertThat(service.applicableAu(DATE)).isEmpty();
    }

    @Test
    void deuxGrillesEnVigueurLeMemeJourSontUneErreur() {
        when(versions.findApplicablesAu(DATE, "AUTO"))
                .thenReturn(List.of(version("a", DATE, null), version("b", DATE, null)));

        assertThatThrownBy(() -> service.applicableAu(DATE))
                .isInstanceOf(GrilleInvalideException.class)
                .hasMessageContaining("Plusieurs");
    }

    @Test
    void verifierChargeToutes() {
        when(versions.findByProduitOrderByDateDebutAsc(eq("AUTO")))
                .thenReturn(List.of(version("AUTO-2026.1", DATE, null)));

        service.verifier();
    }

    @Test
    void sansAucuneGrilleAutoLeControleEchoue() {
        when(versions.findByProduitOrderByDateDebutAsc(eq("AUTO"))).thenReturn(List.of());

        assertThatThrownBy(service::verifier).isInstanceOf(GrilleInvalideException.class);
    }

    @Test
    void uneGrilleAutoSansFormuleEstRefusee() {
        when(versions.findByProduitOrderByDateDebutAsc(eq("AUTO")))
                .thenReturn(List.of(version("AUTO-2026.1", DATE, null)));
        when(formules.findByGrilleIdOrderByOrdre(any())).thenReturn(List.of());

        assertThatThrownBy(service::verifier).isInstanceOf(GrilleInvalideException.class);
    }
}
