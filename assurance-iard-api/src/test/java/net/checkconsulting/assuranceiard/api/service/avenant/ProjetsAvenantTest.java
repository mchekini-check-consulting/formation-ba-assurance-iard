package net.checkconsulting.assuranceiard.api.service.avenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import net.checkconsulting.assuranceiard.api.dto.AvenantResponse;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.dto.FicheContratResponse;
import net.checkconsulting.assuranceiard.api.dto.ProjetAvenantResponse;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.service.EspaceClientService;
import net.checkconsulting.assuranceiard.api.service.facture.EcheancierService;
import net.checkconsulting.assuranceiard.api.service.facture.EcheancierService.Echeancier;
import net.checkconsulting.assuranceiard.api.service.facture.EcheancierService.LigneEcheance;
import org.junit.jupiter.api.Test;

/** US-23, AC3, RG3 et RG4 : ce que l'écran « Relire et accepter l'avenant » affiche. */
class ProjetsAvenantTest {

    private static final LocalDate EFFET_AVENANT = LocalDate.of(2026, 11, 27);

    private final EspaceClientService espaceClient = mock(EspaceClientService.class);
    private final EcheancierService echeancier = mock(EcheancierService.class);
    private final ProjetsAvenant projets = new ProjetsAvenant(espaceClient, echeancier);

    private static AvenantResponse avenant(List<AvenantResponse.Garantie> retraits) {
        return new AvenantResponse("AV-2026-000009", 1, "EN_ATTENTE", EFFET_AVENANT, new BigDecimal("47.50"),
                new BigDecimal("48.50"),
                List.of(new AvenantResponse.Garantie("ASSISTANCE_0KM", "Assistance dès 0 km", new BigDecimal("3.50"))),
                retraits, null, null);
    }

    private static DonneesContrat donnees(String frequence) {
        return new DonneesContrat(3L, "AE-RECETTE-A03", Contrat.AUTO, "TR", null, null, null, null,
                new DonneesContrat.Tarif(null, new BigDecimal("47.50"), new BigDecimal("570.00"),
                        LocalDate.of(2026, 3, 1), null),
                new DonneesContrat.Paiement(frequence, 5, null, null), null, List.of());
    }

    private static LigneEcheance echeance(LocalDate date) {
        return new LigneEcheance(date, new BigDecimal("47.50"), "A_VENIR", date, date.plusMonths(1).minusDays(1));
    }

    @Test
    void ac3RetraitAuto4750Vers4850AvecConseilMisAJourIpidDeLaFormuleEtDocumentsDuContrat() {
        Contrat contrat = CatalogueGarantiesTest.contrat(Contrat.AUTO, "TR");
        contrat.setStatut("AVENANT_EN_ATTENTE");
        DonneesContrat donnees = donnees("MENSUEL");
        when(echeancier.de("AVENANT_EN_ATTENTE", donnees)).thenReturn(new Echeancier(List.of(
                echeance(LocalDate.of(2026, 12, 5)), echeance(LocalDate.of(2026, 11, 5))), List.of()));
        when(espaceClient.documentsDeLaFiche(contrat)).thenReturn(List.of(
                new FicheContratResponse.Document(1L, "MANDAT_SEPA", "Mandat", 1, Instant.EPOCH),
                new FicheContratResponse.Document(2L, "CONDITIONS_GENERALES", "Conditions générales", 1,
                        Instant.EPOCH)));

        ProjetAvenantResponse projet = projets.projet(contrat, donnees, avenant(List.of(
                new AvenantResponse.Garantie("PROTECTION_JURIDIQUE", "Protection juridique", new BigDecimal("2.50")))));

        assertThat(projet.ancienneCotisationAnnuelle()).isEqualByComparingTo("570.00");
        assertThat(projet.nouvelleCotisationAnnuelle()).isEqualByComparingTo("582.00");
        // RG3 : première mensualité qui suit la date d'effet (27/11) : le 05/12.
        assertThat(projet.premiereEcheanceConcernee()).isEqualTo(LocalDate.of(2026, 12, 5));
        // RG4 : conseil mis à jour avant l'acceptation.
        assertThat(projet.reductionCouverture()).isTrue();
        assertThat(projet.conseils()).hasSize(2);
        assertThat(projet.conseils().getFirst()).contains("L521-4");
        assertThat(projet.conseils().get(1)).contains("« Protection juridique »", "frais de litige", "27/11/2026");
        assertThat(projet.ipidFormule()).isEqualTo("TR");
        assertThat(projet.documents()).extracting(FicheContratResponse.Document::type)
                .containsExactly("CONDITIONS_GENERALES");
    }

    @Test
    void sansRetraitPasDeConseilEtHabitationSansIpidDeFormule() {
        Contrat contrat = CatalogueGarantiesTest.contrat(Contrat.HABITATION, "confort");
        contrat.setStatut("ACTIF");
        DonneesContrat donnees = donnees("MENSUEL");
        when(echeancier.de("ACTIF", donnees)).thenReturn(Echeancier.VIDE);
        when(espaceClient.documentsDeLaFiche(contrat)).thenReturn(List.of());

        ProjetAvenantResponse projet = projets.projet(contrat, donnees, avenant(List.of()));

        assertThat(projet.reductionCouverture()).isFalse();
        assertThat(projet.conseils()).isEmpty();
        assertThat(projet.ipidFormule()).isNull();
        assertThat(projet.premiereEcheanceConcernee()).isNull();
    }

    @Test
    void rg3AuDelaDeLEcheancierCalculeLEcheanceSuivanteDansLeMemeRythme() {
        DonneesContrat annuel = donnees("ANNUEL");
        // Paiement annuel : dernière échéance le 05/03/2026 ; la prochaine annuelle est le 05/03/2027.
        when(echeancier.de("ACTIF", annuel)).thenReturn(new Echeancier(List.of(echeance(LocalDate.of(2026, 3, 5))),
                List.of()));
        assertThat(projets.premiereEcheanceConcernee("ACTIF", annuel, EFFET_AVENANT))
                .isEqualTo(LocalDate.of(2027, 3, 5));

        DonneesContrat mensuel = donnees("MENSUEL");
        when(echeancier.de("ACTIF", mensuel)).thenReturn(new Echeancier(List.of(echeance(LocalDate.of(2026, 10, 5))),
                List.of()));
        assertThat(projets.premiereEcheanceConcernee("ACTIF", mensuel, EFFET_AVENANT))
                .isEqualTo(LocalDate.of(2026, 12, 5));
    }
}
