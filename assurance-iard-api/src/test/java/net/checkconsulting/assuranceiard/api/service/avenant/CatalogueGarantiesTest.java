package net.checkconsulting.assuranceiard.api.service.avenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.exception.MoteurIndisponibleException;
import net.checkconsulting.assuranceiard.api.model.Conducteur;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.ContratOption;
import net.checkconsulting.assuranceiard.api.model.Vehicule;
import net.checkconsulting.assuranceiard.api.repository.ConducteurRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratOptionRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.service.FormuleAutoService;
import net.checkconsulting.assuranceiard.api.service.TarifDevisService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** US-23, AC1 et AC2 : garanties actuelles et ajoutables, avec leur prix ; garanties obligatoires non modifiables. */
class CatalogueGarantiesTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 26);

    private final ContratOptionRepository options = mock(ContratOptionRepository.class);
    private final DonneesContratRepository donneesContrat = mock(DonneesContratRepository.class);
    private final ConducteurRepository conducteurs = mock(ConducteurRepository.class);
    private final FormuleAutoService formulesAuto = mock(FormuleAutoService.class);
    private final TarifDevisService tarifsHabitation = mock(TarifDevisService.class);
    private final CatalogueGaranties catalogue = new CatalogueGaranties(options, donneesContrat, conducteurs,
            formulesAuto, tarifsHabitation);

    static Contrat contrat(String produit, String formule) {
        Contrat contrat = new Contrat();
        contrat.setId(3L);
        contrat.setNumero("AE-RECETTE-A03");
        ReflectionTestUtils.setField(contrat, "produit", produit);
        ReflectionTestUtils.setField(contrat, "formule", formule);
        return contrat;
    }

    static DonneesContrat donnees(String produit, List<DonneesContrat.Option> souscrites) {
        return new DonneesContrat(3L, "AE-RECETTE-A03", produit, "TR",
                new DonneesContrat.Souscripteur("madame", "Camille", "Durand"),
                new DonneesContrat.Logement("appartement", 55, "3 rue de la Recette", "33000", "Bordeaux"),
                new DonneesContrat.Vehicule("AB-003-CD", "Renault", "Clio", "V TCe 90"),
                new DonneesContrat.Conducteur(new BigDecimal("0.68"), true),
                new DonneesContrat.Tarif(new BigDecimal("500"), new BigDecimal("47.50"), new BigDecimal("570.00"),
                        LocalDate.of(2026, 9, 27), null),
                new DonneesContrat.Paiement("MENSUEL", 5, "•••• 7788", "AE-RUM-RECETTE-A"), null, souscrites);
    }

    private static ContratOption obligatoire(String code) {
        ContratOption option = new ContratOption();
        option.setCodeOption(code);
        option.setObligatoire(true);
        return option;
    }

    @Test
    void ac1Ac2AutoGarantiesSouscritesPuisOptionsDeLaGrilleT04AvecLaRcObligatoire() {
        Contrat contrat = contrat(Contrat.AUTO, "TR");
        contrat.setVehicule(new Vehicule());
        Conducteur conducteur = new Conducteur();
        when(donneesContrat.conducteurPrincipal(3L)).thenReturn(Optional.of(7L));
        when(conducteurs.findById(7L)).thenReturn(Optional.of(conducteur));
        when(options.enCours(3L, AUJOURDHUI)).thenReturn(List.of(obligatoire("RC")));
        Map<String, BigDecimal> grille = new LinkedHashMap<>();
        grille.put("ASSISTANCE_0KM", new BigDecimal("3.50"));
        grille.put("PROTECTION_JURIDIQUE", new BigDecimal("2.50"));
        when(formulesAuto.prixDesOptions(contrat.getVehicule(), conducteur, "TR")).thenReturn(grille);

        List<GarantieProposee> garanties = catalogue.garanties(contrat, donnees(Contrat.AUTO, List.of(
                new DonneesContrat.Option("RC", new BigDecimal("12.00")),
                new DonneesContrat.Option("PROTECTION_JURIDIQUE", new BigDecimal("2.40")))), AUJOURDHUI);

        assertThat(garanties).extracting(GarantieProposee::code)
                .containsExactly("RC", "PROTECTION_JURIDIQUE", "ASSISTANCE_0KM");
        // AC2 : la RC n'est pas modifiable et la raison est expliquée.
        assertThat(garanties.getFirst()).satisfies(rc -> {
            assertThat(rc.obligatoire()).isTrue();
            assertThat(rc.modifiable()).isFalse();
            assertThat(rc.raison()).isEqualTo(CatalogueGaranties.RAISON_RESPONSABILITE_CIVILE);
            assertThat(rc.libelle()).isEqualTo("Responsabilité civile");
        });
        // Une garantie souscrite garde le prix payé par le client.
        assertThat(garanties.get(1).prixMensuelTtc()).isEqualByComparingTo("2.40");
        assertThat(garanties.get(1).souscrite()).isTrue();
        assertThat(garanties.get(2)).satisfies(option -> {
            assertThat(option.souscrite()).isFalse();
            assertThat(option.modifiable()).isTrue();
            assertThat(option.prixMensuelTtc()).isEqualByComparingTo("3.50");
        });
        verifyNoInteractions(tarifsHabitation);
    }

    @Test
    void ac1HabitationDuSprint1LesOptionsDuDevisPrennentLePrixDeLaGrille() {
        Contrat contrat = contrat(Contrat.HABITATION, "confort");
        DonneesContrat donnees = donnees(Contrat.HABITATION,
                List.of(new DonneesContrat.Option("objets-valeur", null)));
        Map<String, BigDecimal> grille = new LinkedHashMap<>();
        grille.put("objets-valeur", new BigDecimal("2.50"));
        grille.put("assistance-depannage", new BigDecimal("1.50"));
        when(tarifsHabitation.prixDesOptions(donnees)).thenReturn(grille);

        List<GarantieProposee> garanties = catalogue.garanties(contrat, donnees, AUJOURDHUI);

        assertThat(garanties).extracting(GarantieProposee::code, GarantieProposee::souscrite)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("objets-valeur", true),
                        org.assertj.core.groups.Tuple.tuple("assistance-depannage", false));
        assertThat(garanties.getFirst().prixMensuelTtc()).isEqualByComparingTo("2.50");
        assertThat(garanties).allMatch(GarantieProposee::modifiable);
        verifyNoInteractions(formulesAuto);
    }

    @Test
    void ac2RaisonDUneGarantieNonModifiable() {
        assertThat(CatalogueGaranties.raison("ASSISTANCE", new BigDecimal("1.00"), true))
                .isEqualTo(CatalogueGaranties.RAISON_OBLIGATOIRE);
        assertThat(CatalogueGaranties.raison("VOL", null, false)).isEqualTo(CatalogueGaranties.RAISON_SANS_PRIX);
        assertThat(CatalogueGaranties.raison("VOL", BigDecimal.ONE, false)).isNull();
    }

    @Test
    void autoSansConducteurLesPrixSontIndisponibles() {
        when(donneesContrat.conducteurPrincipal(any(Long.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogue.garanties(contrat(Contrat.AUTO, "TR"), donnees(Contrat.AUTO, List.of()),
                AUJOURDHUI)).isInstanceOf(MoteurIndisponibleException.class);
    }
}
