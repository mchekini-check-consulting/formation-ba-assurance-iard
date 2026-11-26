package net.checkconsulting.assuranceiard.api.service.facture;

import static net.checkconsulting.assuranceiard.api.service.facture.ContratsFictifs.habitation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.DemandeFactureRequest;
import net.checkconsulting.assuranceiard.api.dto.DemandeFactureRequest.Periode;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.dto.FactureGenereeResponse;
import net.checkconsulting.assuranceiard.api.dto.MesFacturesResponse;
import net.checkconsulting.assuranceiard.api.exception.ContratInconnuException;
import net.checkconsulting.assuranceiard.api.exception.FactureImpossibleException;
import net.checkconsulting.assuranceiard.api.exception.PeriodeFacturationInconnueException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.Facture;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.repository.FactureRepository;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import net.checkconsulting.assuranceiard.api.service.document.DemandeDocument;
import net.checkconsulting.assuranceiard.api.service.document.DocumentsGeneresService;
import net.checkconsulting.assuranceiard.api.service.facture.EcheancierService.AnneeCotisation;
import net.checkconsulting.assuranceiard.api.service.facture.EcheancierService.Echeancier;
import net.checkconsulting.assuranceiard.api.service.facture.EcheancierService.LigneEcheance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FactureServiceTest {

    private static final String JETON = "Bearer jeton";
    private static final String NUMERO = "AE-2026-000012";
    private static final long CONTRAT = ContratsFictifs.ID;
    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneId.of("Europe/Paris"));
    private static final LocalDate EFFET = LocalDate.of(2026, 3, 1);
    private static final LocalDate FIN_ANNEE = LocalDate.of(2027, 2, 28);
    private static final LocalDate A_VENIR = LocalDate.of(2026, 12, 10);
    private static final LocalDate ECHEANCE = LocalDate.of(2026, 11, 10);
    private static final LocalDate OCTOBRE = LocalDate.of(2026, 10, 10);
    private static final DonneesContrat DONNEES = habitation(EFFET, null, 10);
    private static final BigDecimal MENSUALITE = new BigDecimal("18.50");
    private static final BigDecimal ANNUELLE = new BigDecimal("222.00");
    /** Année en cours : 2 échéances payées, 1 à venir (non réglée). */
    private static final Echeancier ECHEANCIER = new Echeancier(
            List.of(new LigneEcheance(A_VENIR, MENSUALITE, "A_VENIR", A_VENIR, LocalDate.of(2027, 1, 9)),
                    new LigneEcheance(ECHEANCE, MENSUALITE, "PAYEE", ECHEANCE, LocalDate.of(2026, 12, 9)),
                    new LigneEcheance(OCTOBRE, MENSUALITE, "PAYEE", OCTOBRE, LocalDate.of(2026, 11, 9))),
            List.of(new AnneeCotisation(EFFET, FIN_ANNEE, ANNUELLE, false)));
    /** Année précédente entièrement réglée. */
    private static final LocalDate ANNEE_REGLEE = LocalDate.of(2025, 3, 1);
    private static final Echeancier ECHEANCIER_REGLE = new Echeancier(List.of(),
            List.of(new AnneeCotisation(ANNEE_REGLEE, LocalDate.of(2026, 2, 28), ANNUELLE, true)));

    @Mock
    private SessionService sessionService;

    @Mock
    private ContratRepository contrats;

    @Mock
    private DonneesContratRepository donneesContrat;

    @Mock
    private FactureRepository factures;

    @Mock
    private DocumentRepository documents;

    @Mock
    private EcheancierService echeancier;

    @Mock
    private DocumentsGeneresService documentsGeneres;

    private FactureService service;

    @BeforeEach
    void preparer() {
        service = new FactureService(sessionService, contrats, donneesContrat, factures, documents, echeancier,
                new CalculFacture(CalculFactureTest.REGLAGES), documentsGeneres, HORLOGE);
    }

    private static Contrat contrat(long id, String numero, String statut, LocalDate fin) {
        Contrat contrat = new Contrat();
        contrat.setId(id);
        contrat.setNumero(numero);
        contrat.setStatut(statut);
        contrat.setProduit("HABITATION");
        contrat.setAdresseLogement("12 rue des Lilas");
        contrat.setCodePostal("75001");
        contrat.setVille("Paris");
        contrat.setDateEffet(EFFET);
        contrat.setDateFin(fin);
        return contrat;
    }

    private static Facture facture(long id, Long documentId, LocalDate debut, LocalDate fin) {
        Facture facture = new Facture();
        facture.setId(id);
        facture.setNumero("FAC-2026-%06d".formatted(id));
        facture.setContratId(CONTRAT);
        facture.setPeriodeDebut(debut);
        facture.setPeriodeFin(fin);
        facture.setMontantHt(new BigDecimal("16.97"));
        facture.setTaxe(new BigDecimal("1.53"));
        facture.setMontantTtc(MENSUALITE);
        facture.setGenereeLe(Instant.parse("2026-11-20T10:00:00Z"));
        facture.setDocumentId(documentId);
        return facture;
    }

    /** Facture de l'échéance du 10/11 (même période). */
    private static Facture factureNovembre(long id, Long documentId) {
        return facture(id, documentId, ECHEANCE, LocalDate.of(2026, 12, 9));
    }

    /** Comme FAC-RECETTE-0001 : un mois civil qui couvre l'échéance du 10/10. */
    private static Facture factureMoisCivilOctobre() {
        return facture(1, 10L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    }

    private static Document document(long id, boolean stocke) {
        Document document = new Document();
        document.setId(id);
        document.setStocke(stocke);
        return document;
    }

    private void contratActif(String statut, LocalDate fin, Echeancier sonEcheancier) {
        when(contrats.parNumero(NUMERO)).thenReturn(Optional.of(contrat(CONTRAT, NUMERO, statut, fin)));
        when(donneesContrat.lire(CONTRAT)).thenReturn(Optional.of(DONNEES));
        when(echeancier.de(statut, DONNEES)).thenReturn(sonEcheancier);
    }

    private void facturesExistantes(Facture... existantes) {
        when(factures.findByContratIdInOrderByPeriodeFinDescIdDesc(List.of(CONTRAT))).thenReturn(List.of(existantes));
    }

    private void nouvelleFacture(long id, Document pdf) {
        when(factures.reserverIdentifiant()).thenReturn(id);
        when(documentsGeneres.generer(any())).thenReturn(pdf);
        when(factures.findById(id)).thenReturn(Optional.of(factureNovembre(id, null)));
    }

    private FactureGenereeResponse generer(Periode periode, LocalDate date) {
        return service.generer(NUMERO, new DemandeFactureRequest(periode, date));
    }

    // --- Mes factures ---

    @Test
    void ac1Ac2EcheancierEtFacturesDesContratsSignesSeulement() {
        Compte compte = new Compte();
        when(sessionService.compteConnecte(JETON)).thenReturn(compte);
        when(contrats.duClient(compte)).thenReturn(List.of(contrat(CONTRAT, NUMERO, "ACTIF", null),
                contrat(99L, "AE-2026-000099", "EN_ATTENTE_DE_SIGNATURE", null)));
        facturesExistantes(factureNovembre(2, 20L), factureMoisCivilOctobre());
        when(documents.findAllById(List.of(20L, 10L))).thenReturn(List.of(document(20, true)));
        when(donneesContrat.lire(CONTRAT)).thenReturn(Optional.of(DONNEES));
        when(echeancier.de("ACTIF", DONNEES)).thenReturn(ECHEANCIER);

        MesFacturesResponse reponse = service.mesFactures(JETON);

        assertThat(reponse.aucuneCotisation()).isFalse();
        assertThat(reponse.contrats()).singleElement().satisfies(un -> {
            assertThat(un.numero()).isEqualTo(NUMERO);
            assertThat(un.bienAssure()).isEqualTo("12 rue des Lilas, 75001 Paris");
            // Recette : seule une échéance payée se facture ; une facture qui la couvre déjà est indiquée.
            assertThat(un.echeances()).extracting(MesFacturesResponse.Echeance::facturable)
                    .containsExactly(false, true, true);
            assertThat(un.echeances()).extracting(MesFacturesResponse.Echeance::facture)
                    .containsExactly(null, "FAC-2026-000002", "FAC-2026-000001");
            // L'année n'est pas entièrement réglée : pas de facture annuelle.
            assertThat(un.annees()).singleElement().satisfies(annee -> {
                assertThat(annee.montantTtc()).isEqualByComparingTo("222.00");
                assertThat(annee.facturable()).isFalse();
                assertThat(annee.reglee()).isFalse();
                assertThat(annee.facture()).isNull();
            });
            // Téléchargeable seulement si le PDF est stocké.
            assertThat(un.factures()).extracting(MesFacturesResponse.Facture::documentId).containsExactly(20L, null);
        });
    }

    @Test
    void uneAnneeRegleeEstFacturableEtSaFactureEstIndiquee() {
        Compte compte = new Compte();
        when(sessionService.compteConnecte(JETON)).thenReturn(compte);
        when(contrats.duClient(compte)).thenReturn(List.of(contrat(CONTRAT, NUMERO, "ACTIF", null)));
        Facture annuelle = facture(5, null, ANNEE_REGLEE, LocalDate.of(2026, 2, 28));
        facturesExistantes(annuelle);
        when(donneesContrat.lire(CONTRAT)).thenReturn(Optional.of(DONNEES));
        when(echeancier.de("ACTIF", DONNEES)).thenReturn(ECHEANCIER_REGLE);

        MesFacturesResponse.Annee annee = service.mesFactures(JETON).contrats().getFirst().annees().getFirst();

        assertThat(annee.facturable()).isTrue();
        assertThat(annee.reglee()).isTrue();
        assertThat(annee.facture()).isEqualTo("FAC-2026-000005");
    }

    @Test
    void ac4AucuneCotisation() {
        Compte compte = new Compte();
        when(sessionService.compteConnecte(JETON)).thenReturn(compte);
        when(contrats.duClient(compte)).thenReturn(List.of(contrat(CONTRAT, NUMERO, "EN_ATTENTE_EFFET", null)));
        facturesExistantes();
        when(donneesContrat.lire(CONTRAT)).thenReturn(Optional.empty());

        MesFacturesResponse reponse = service.mesFactures(JETON);

        assertThat(reponse.aucuneCotisation()).isTrue();
        assertThat(reponse.contrats()).singleElement().satisfies(un -> assertThat(un.echeances()).isEmpty());
        verifyNoInteractions(documents);
    }

    @Test
    void ac4SansContratAucuneCotisation() {
        Compte compte = new Compte();
        when(sessionService.compteConnecte(JETON)).thenReturn(compte);
        when(contrats.duClient(compte)).thenReturn(List.of());

        assertThat(service.mesFactures(JETON)).isEqualTo(new MesFacturesResponse(List.of(), true));
        verifyNoInteractions(factures);
    }

    @Test
    void contratResilieLesPeriodesApresSaFinNeSontPasFacturables() {
        Compte compte = new Compte();
        when(sessionService.compteConnecte(JETON)).thenReturn(compte);
        when(contrats.duClient(compte)).thenReturn(List.of(contrat(CONTRAT, NUMERO, "RESILIE",
                LocalDate.of(2026, 10, 31))));
        facturesExistantes();
        when(donneesContrat.lire(CONTRAT)).thenReturn(Optional.of(DONNEES));
        when(echeancier.de("RESILIE", DONNEES)).thenReturn(ECHEANCIER);

        MesFacturesResponse.Contrat un = service.mesFactures(JETON).contrats().getFirst();

        assertThat(un.echeances()).extracting(MesFacturesResponse.Echeance::facturable)
                .containsExactly(false, false, true);
    }

    // --- Générer une facture ---

    @Test
    void ac2GenereLaFactureDUneEcheancePayeeNumeroteeEtStockee() {
        contratActif("ACTIF", null, ECHEANCIER);
        facturesExistantes();
        nouvelleFacture(42L, document(77, true));

        FactureGenereeResponse reponse = generer(Periode.ECHEANCE, ECHEANCE);

        // RG4 : FAC-AAAA-NNNNNN. 18,50 € TTC à 9 % : 16,97 € HT + 1,53 € de TSCA.
        verify(factures).verrouillerContrat(CONTRAT);
        verify(factures).inserer(42L, "FAC-2026-000042", CONTRAT, ECHEANCE, LocalDate.of(2026, 12, 9),
                new BigDecimal("16.97"), new BigDecimal("1.53"), MENSUALITE, HORLOGE.instant());
        ArgumentCaptor<DemandeDocument> demande = ArgumentCaptor.forClass(DemandeDocument.class);
        verify(documentsGeneres).generer(demande.capture());
        assertThat(demande.getValue().reference()).isEqualTo("FAC-2026-000042");
        assertThat(demande.getValue().contratId()).isEqualTo(CONTRAT);
        assertThat(reponse.existante()).isFalse();
        assertThat(reponse.disponible()).isTrue();
        assertThat(reponse.facture().documentId()).isEqualTo(77L);
    }

    @Test
    void recettePoint2PasDeFacturePourUneEcheanceAVenir() {
        contratActif("ACTIF", null, ECHEANCIER);
        facturesExistantes();

        assertThatThrownBy(() -> generer(Periode.ECHEANCE, A_VENIR))
                .isInstanceOf(FactureImpossibleException.class)
                .hasMessage(FactureService.MESSAGE_ECHEANCE_NON_PAYEE);
        verify(factures, never()).reserverIdentifiant();
    }

    @Test
    void recettePoint2PasDeFactureAnnuelleAvantQueLAnneeSoitReglee() {
        contratActif("ACTIF", null, ECHEANCIER);
        facturesExistantes();

        assertThatThrownBy(() -> generer(Periode.ANNEE, EFFET))
                .isInstanceOf(FactureImpossibleException.class)
                .hasMessage(FactureService.MESSAGE_ANNEE_NON_REGLEE);
        verify(factures, never()).reserverIdentifiant();
    }

    @Test
    void recettePoint1LaFactureDUneAnneeRegleeVautLaCotisationAnnuelle() {
        contratActif("ACTIF", null, ECHEANCIER_REGLE);
        facturesExistantes();
        nouvelleFacture(43L, document(78, false));

        FactureGenereeResponse reponse = generer(Periode.ANNEE, ANNEE_REGLEE);

        verify(factures).inserer(eq(43L), eq("FAC-2026-000043"), eq(CONTRAT), eq(ANNEE_REGLEE),
                eq(LocalDate.of(2026, 2, 28)), any(), any(), eq(ANNUELLE), any());
        // PDF pas encore produit : la reprise de T-03 le stockera.
        assertThat(reponse.disponible()).isFalse();
        assertThat(reponse.facture().documentId()).isNull();
    }

    @Test
    void ac2UneFactureDeLaMemePeriodeEstRenvoyeeSansNouveauNumero() {
        contratActif("ACTIF", null, ECHEANCIER);
        facturesExistantes(factureNovembre(7, 70L));
        when(documents.findAllById(List.of(70L))).thenReturn(List.of(document(70, true)));

        FactureGenereeResponse reponse = generer(Periode.ECHEANCE, ECHEANCE);

        assertThat(reponse.existante()).isTrue();
        assertThat(reponse.disponible()).isTrue();
        assertThat(reponse.facture().numero()).isEqualTo("FAC-2026-000007");
        verify(factures, never()).reserverIdentifiant();
        verifyNoInteractions(documentsGeneres);
    }

    @Test
    void recettePoint3UneFactureDuMoisQuiCouvreLEcheanceEstRenvoyee() {
        contratActif("ACTIF", null, ECHEANCIER);
        facturesExistantes(factureMoisCivilOctobre());
        when(documents.findAllById(List.of(10L))).thenReturn(List.of(document(10, false)));

        FactureGenereeResponse reponse = generer(Periode.ECHEANCE, OCTOBRE);

        assertThat(reponse.existante()).isTrue();
        assertThat(reponse.facture().numero()).isEqualTo("FAC-2026-000001");
        assertThat(reponse.disponible()).isFalse();
        verify(factures, never()).reserverIdentifiant();
    }

    @Test
    void recettePoint3UneFactureAnnuelleNeCouvrePasUneEcheanceMensuelle() {
        contratActif("ACTIF", null, ECHEANCIER);
        facturesExistantes(facture(5, null, EFFET, FIN_ANNEE));
        nouvelleFacture(44L, document(79, true));

        assertThat(generer(Periode.ECHEANCE, ECHEANCE).existante()).isFalse();
        verify(factures).reserverIdentifiant();
    }

    @Test
    void uneFactureDejaGenereePourUneEcheanceAVenirResteTelechargeable() {
        contratActif("ACTIF", null, ECHEANCIER);
        facturesExistantes(facture(8, null, A_VENIR, LocalDate.of(2027, 1, 9)));

        assertThat(generer(Periode.ECHEANCE, A_VENIR).existante()).isTrue();
    }

    @Test
    void pasDeFacturePourUnContratEnAttenteDeSignature() {
        when(contrats.parNumero(NUMERO)).thenReturn(Optional.of(contrat(CONTRAT, NUMERO, "EN_ATTENTE_DE_SIGNATURE",
                null)));

        assertThatThrownBy(() -> generer(Periode.ECHEANCE, ECHEANCE))
                .isInstanceOf(FactureImpossibleException.class);
        verifyNoInteractions(factures, documentsGeneres);
    }

    @Test
    void contratResiliePasDeFactureApresSaDateDeFin() {
        contratActif("RESILIE", LocalDate.of(2026, 10, 31), ECHEANCIER);

        assertThatThrownBy(() -> generer(Periode.ECHEANCE, ECHEANCE))
                .isInstanceOf(FactureImpossibleException.class);
        verify(factures, never()).reserverIdentifiant();
    }

    @Test
    void contratRenonceSansDateDeFinLimiteAAujourdhui() {
        contratActif("RENONCE", null, ECHEANCIER);
        facturesExistantes(factureNovembre(7, null));

        assertThat(generer(Periode.ECHEANCE, ECHEANCE).existante()).isTrue();
    }

    @Test
    void dateQuiNEstNiUneEcheanceNiUneAnnee404() {
        contratActif("ACTIF", null, ECHEANCIER);

        assertThatThrownBy(() -> generer(Periode.ECHEANCE, LocalDate.of(2026, 11, 11)))
                .isInstanceOf(PeriodeFacturationInconnueException.class);
        assertThatThrownBy(() -> generer(Periode.ANNEE, LocalDate.of(2025, 3, 1)))
                .isInstanceOf(PeriodeFacturationInconnueException.class);
        verify(factures, never()).inserer(anyLong(), any(), anyLong(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void contratInconnu() {
        when(contrats.parNumero("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generer("X", new DemandeFactureRequest(Periode.ECHEANCE, ECHEANCE)))
                .isInstanceOf(ContratInconnuException.class);
    }

    @Test
    void rg4NumeroSurSixChiffres() {
        assertThat(FactureService.numero(2026, 1)).isEqualTo("FAC-2026-000001");
        assertThat(FactureService.numero(2027, 1234567)).isEqualTo("FAC-2027-1234567");
    }

    @Test
    void periodeDAuPlusUnMois() {
        assertThat(FactureService.estMensuelle(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))).isTrue();
        assertThat(FactureService.estMensuelle(ECHEANCE, LocalDate.of(2026, 12, 9))).isTrue();
        assertThat(FactureService.estMensuelle(ECHEANCE, LocalDate.of(2026, 12, 10))).isFalse();
        assertThat(FactureService.estMensuelle(EFFET, FIN_ANNEE)).isFalse();
    }
}
