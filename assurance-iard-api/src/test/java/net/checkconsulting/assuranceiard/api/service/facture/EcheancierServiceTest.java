package net.checkconsulting.assuranceiard.api.service.facture;

import static net.checkconsulting.assuranceiard.api.service.facture.DonneesContratDeTest.ID;
import static net.checkconsulting.assuranceiard.api.service.facture.DonneesContratDeTest.contrat;
import static net.checkconsulting.assuranceiard.api.service.facture.DonneesContratDeTest.habitation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.Echeance;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.repository.EcheanceRepository;
import net.checkconsulting.assuranceiard.api.service.PremierPrelevementService;
import net.checkconsulting.assuranceiard.api.service.facture.EcheancierService.AnneeCotisation;
import net.checkconsulting.assuranceiard.api.service.facture.EcheancierService.Echeancier;
import net.checkconsulting.assuranceiard.api.service.facture.EcheancierService.LigneEcheance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EcheancierServiceTest {

    /** Aujourd'hui : 25/11/2026. */
    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneId.of("Europe/Paris"));
    private static final LocalDate EFFET = LocalDate.of(2026, 3, 1);

    @Mock
    private EcheanceRepository echeances;

    @Mock
    private DonneesContratRepository donneesContrat;

    private EcheancierService service;

    @BeforeEach
    void preparer() {
        service = new EcheancierService(echeances, donneesContrat, new PremierPrelevementService(), HORLOGE);
    }

    private static LocalDate date(int annee, int mois, int jour) {
        return LocalDate.of(annee, mois, jour);
    }

    @Test
    void ac1EnAttenteDeSignaturePasDEcheancier() {
        Echeancier echeancier = service.de("EN_ATTENTE_DE_SIGNATURE", habitation(EFFET, null, 10));

        assertThat(echeancier.estVide()).isTrue();
        assertThat(echeancier.annees()).isEmpty();
        verifyNoInteractions(echeances, donneesContrat);
    }

    @Test
    void ac1MensuelCalculeDepuisLaSignatureJusquALaFinDeLAnneeDAssurance() {
        when(echeances.findByContratIdOrderByDateEcheanceDescIdDesc(ID)).thenReturn(List.of());
        // Signé le 20/02 : premier prélèvement le 10/03 (au moins 14 jours après la signature, RG3 d'US-14).
        when(donneesContrat.dateSignature(ID)).thenReturn(Optional.of(date(2026, 2, 20)));

        Echeancier echeancier = service.de("ACTIF", habitation(EFFET, null, 10));

        // Du 10/03/2026 au 10/02/2027 (l'année d'assurance finit le 28/02/2027), du plus récent au plus ancien.
        assertThat(echeancier.echeances()).hasSize(12);
        assertThat(echeancier.echeances().getFirst().date()).isEqualTo(date(2027, 2, 10));
        assertThat(echeancier.echeances().getLast().date()).isEqualTo(date(2026, 3, 10));
        // Payée si la date est passée, À venir sinon.
        assertThat(echeancier.echeances()).filteredOn(ligne -> "PAYEE".equals(ligne.statut())).hasSize(9);
        assertThat(echeancier.echeances()).filteredOn(ligne -> "A_VENIR".equals(ligne.statut())).hasSize(3);
        assertThat(echeancier.echeances()).filteredOn(ligne -> ligne.date().equals(date(2026, 11, 10)))
                .singleElement().satisfies(ligne -> {
                    assertThat(ligne.montantTtc()).isEqualByComparingTo("18.50");
                    assertThat(ligne.periodeDebut()).isEqualTo(date(2026, 11, 10));
                    assertThat(ligne.periodeFin()).isEqualTo(date(2026, 12, 9));
                });
        assertThat(echeancier.annees()).singleElement().isEqualTo(
                new AnneeCotisation(EFFET, date(2027, 2, 28), new BigDecimal("222.00")));
    }

    @Test
    void ac1ContratResilieAucuneEcheanceApresSaFin() {
        when(echeances.findByContratIdOrderByDateEcheanceDescIdDesc(ID)).thenReturn(List.of());
        when(donneesContrat.dateSignature(ID)).thenReturn(Optional.of(date(2026, 2, 20)));

        Echeancier echeancier = service.de("RESILIE", habitation(EFFET, date(2026, 6, 30), 10));

        assertThat(echeancier.echeances()).extracting(LigneEcheance::date)
                .containsExactly(date(2026, 6, 10), date(2026, 5, 10), date(2026, 4, 10), date(2026, 3, 10));
        assertThat(echeancier.echeances().getFirst().periodeFin()).isEqualTo(date(2026, 6, 30));
        assertThat(echeancier.annees()).singleElement().satisfies(annee -> {
            assertThat(annee.fin()).isEqualTo(date(2026, 6, 30));
            assertThat(annee.montantTtc()).isEqualByComparingTo("74.00");
        });
    }

    @Test
    void ac1SansSignatureLePremierJourDePrelevementApresLEffet() {
        when(echeances.findByContratIdOrderByDateEcheanceDescIdDesc(ID)).thenReturn(List.of());
        when(donneesContrat.dateSignature(ID)).thenReturn(Optional.empty());

        Echeancier echeancier = service.de("ACTIF", habitation(date(2026, 3, 20), null, 15));

        assertThat(echeancier.echeances().getLast().date()).isEqualTo(date(2026, 4, 15));
    }

    @Test
    void ac1AnnuelUneEcheanceParAnneeDAssurance() {
        when(echeances.findByContratIdOrderByDateEcheanceDescIdDesc(ID)).thenReturn(List.of());

        Echeancier echeancier = service.de("ACTIF",
                contrat("AUTO", date(2025, 1, 15), null, "ANNUEL", null, "40.00", List.of()));

        assertThat(echeancier.echeances()).extracting(LigneEcheance::date)
                .containsExactly(date(2026, 1, 15), date(2025, 1, 15));
        assertThat(echeancier.echeances().getFirst().periodeFin()).isEqualTo(date(2027, 1, 14));
        assertThat(echeancier.echeances()).allMatch(ligne -> "PAYEE".equals(ligne.statut()));
        assertThat(echeancier.annees()).extracting(AnneeCotisation::debut)
                .containsExactly(date(2026, 1, 15), date(2025, 1, 15));
        assertThat(echeancier.annees().getFirst().montantTtc()).isEqualByComparingTo("480.00");
    }

    @Test
    void ac1LesEcheancesEnregistreesPriment() {
        Echeance impayee = echeance(date(2026, 10, 10), "IMPAYEE");
        Echeance aVenir = echeance(date(2026, 12, 10), "A_VENIR");
        when(echeances.findByContratIdOrderByDateEcheanceDescIdDesc(ID)).thenReturn(List.of(impayee, aVenir));

        Echeancier echeancier = service.de("IMPAYE", habitation(EFFET, null, 10));

        assertThat(echeancier.echeances()).extracting(LigneEcheance::date, LigneEcheance::statut)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(date(2026, 12, 10), "A_VENIR"),
                        org.assertj.core.groups.Tuple.tuple(date(2026, 10, 10), "IMPAYEE"));
        verifyNoInteractions(donneesContrat);
    }

    @Test
    void ac1SansDateDEffetNiCotisationPasDEcheance() {
        assertThat(service.de("ACTIF", habitation(null, null, 10)).estVide()).isTrue();

        when(echeances.findByContratIdOrderByDateEcheanceDescIdDesc(ID)).thenReturn(List.of());
        assertThat(service.de("ACTIF", DonneesContratDeTest.sansCotisation(EFFET)).estVide()).isTrue();
    }

    @Test
    void anneeDAssuranceDUneDate() {
        assertThat(EcheancierService.debutAnnee(EFFET, date(2027, 2, 28))).isEqualTo(EFFET);
        assertThat(EcheancierService.debutAnnee(EFFET, date(2027, 3, 1))).isEqualTo(date(2027, 3, 1));
        assertThat(EcheancierService.debutAnnee(EFFET, date(2026, 1, 10))).isEqualTo(date(2025, 3, 1));
        // Effet au 29 février : l'anniversaire tombe le 28 février.
        assertThat(EcheancierService.debutAnnee(date(2024, 2, 29), date(2025, 2, 28))).isEqualTo(date(2025, 2, 28));
        assertThat(EcheancierService.finAnnee(EFFET, date(2026, 11, 25))).isEqualTo(date(2027, 2, 28));
    }

    private static Echeance echeance(LocalDate date, String statut) {
        Echeance echeance = new Echeance();
        echeance.setContratId(ID);
        echeance.setDateEcheance(date);
        echeance.setMontantHt(new BigDecimal("16.97"));
        echeance.setTaxe(new BigDecimal("1.53"));
        echeance.setMontantTtc(new BigDecimal("18.50"));
        echeance.setStatut(statut);
        return echeance;
    }
}
