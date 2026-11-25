package net.checkconsulting.assuranceiard.api.service.facture;

import static net.checkconsulting.assuranceiard.api.service.facture.DonneesContratDeTest.habitation;
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
    private static final long CONTRAT = DonneesContratDeTest.ID;
    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneId.of("Europe/Paris"));
    private static final LocalDate EFFET = LocalDate.of(2026, 3, 1);
    private static final LocalDate ECHEANCE = LocalDate.of(2026, 11, 10);
    private static final DonneesContrat DONNEES = habitation(EFFET, null, 10);
    private static final Echeancier ECHEANCIER = new Echeancier(
            List.of(new LigneEcheance(ECHEANCE, new BigDecimal("18.50"), "PAYEE", ECHEANCE, LocalDate.of(2026, 12, 9)),
                    new LigneEcheance(LocalDate.of(2026, 10, 10), new BigDecimal("18.50"), "PAYEE",
                            LocalDate.of(2026, 10, 10), LocalDate.of(2026, 11, 9))),
            List.of(new AnneeCotisation(EFFET, LocalDate.of(2027, 2, 28), new BigDecimal("37.00"))));

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

    private static Facture facture(long id, Long documentId) {
        Facture facture = new Facture();
        facture.setId(id);
        facture.setNumero("FAC-2026-%06d".formatted(id));
        facture.setContratId(CONTRAT);
        facture.setPeriodeDebut(ECHEANCE);
        facture.setPeriodeFin(LocalDate.of(2026, 12, 9));
        facture.setMontantHt(new BigDecimal("16.97"));
        facture.setTaxe(new BigDecimal("1.53"));
        facture.setMontantTtc(new BigDecimal("18.50"));
        facture.setGenereeLe(Instant.parse("2026-11-20T10:00:00Z"));
        facture.setDocumentId(documentId);
        return facture;
    }

    private static Document document(long id, boolean stocke) {
        Document document = new Document();
        document.setId(id);
        document.setStocke(stocke);
        return document;
    }

    private void contratActif(String statut, LocalDate fin) {
        when(contrats.parNumero(NUMERO)).thenReturn(Optional.of(contrat(CONTRAT, NUMERO, statut, fin)));
        when(donneesContrat.lire(CONTRAT)).thenReturn(Optional.of(DONNEES));
        when(echeancier.de(statut, DONNEES)).thenReturn(ECHEANCIER);
    }

    // --- Mes factures ---

    @Test
    void ac1Ac2EcheancierEtFacturesDesContratsSignesSeulement() {
        Compte compte = new Compte();
        when(sessionService.compteConnecte(JETON)).thenReturn(compte);
        Contrat actif = contrat(CONTRAT, NUMERO, "ACTIF", null);
        when(contrats.duClient(compte)).thenReturn(List.of(actif,
                contrat(99L, "AE-2026-000099", "EN_ATTENTE_DE_SIGNATURE", null)));
        when(factures.findByContratIdInOrderByPeriodeFinDescIdDesc(List.of(CONTRAT)))
                .thenReturn(List.of(facture(2, 20L), facture(1, null)));
        when(documents.findAllById(List.of(20L))).thenReturn(List.of(document(20, true)));
        when(donneesContrat.lire(CONTRAT)).thenReturn(Optional.of(DONNEES));
        when(echeancier.de("ACTIF", DONNEES)).thenReturn(ECHEANCIER);

        MesFacturesResponse reponse = service.mesFactures(JETON);

        assertThat(reponse.aucuneCotisation()).isFalse();
        assertThat(reponse.contrats()).singleElement().satisfies(un -> {
            assertThat(un.numero()).isEqualTo(NUMERO);
            assertThat(un.bienAssure()).isEqualTo("12 rue des Lilas, 75001 Paris");
            assertThat(un.echeances()).hasSize(2).allMatch(MesFacturesResponse.Echeance::facturable);
            assertThat(un.echeances().getFirst().date()).isEqualTo(ECHEANCE);
            assertThat(un.annees()).singleElement().satisfies(annee -> assertThat(annee.facturable()).isTrue());
            // Téléchargeable seulement si le PDF est stocké.
            assertThat(un.factures()).extracting(MesFacturesResponse.Facture::documentId).containsExactly(20L, null);
        });
    }

    @Test
    void ac4AucuneCotisation() {
        Compte compte = new Compte();
        when(sessionService.compteConnecte(JETON)).thenReturn(compte);
        when(contrats.duClient(compte)).thenReturn(List.of(contrat(CONTRAT, NUMERO, "EN_ATTENTE_EFFET", null)));
        when(factures.findByContratIdInOrderByPeriodeFinDescIdDesc(List.of(CONTRAT))).thenReturn(List.of());
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
        when(factures.findByContratIdInOrderByPeriodeFinDescIdDesc(List.of(CONTRAT))).thenReturn(List.of());
        when(donneesContrat.lire(CONTRAT)).thenReturn(Optional.of(DONNEES));
        when(echeancier.de("RESILIE", DONNEES)).thenReturn(ECHEANCIER);

        MesFacturesResponse.Contrat un = service.mesFactures(JETON).contrats().getFirst();

        assertThat(un.echeances()).extracting(MesFacturesResponse.Echeance::facturable).containsExactly(false, true);
        assertThat(un.annees().getFirst().facturable()).isTrue();
    }

    // --- Générer une facture ---

    @Test
    void ac2GenereLaFactureDUneEcheanceNumeroteeEtStockee() {
        contratActif("ACTIF", null);
        when(factures.findFirstByContratIdAndPeriodeDebutAndPeriodeFinOrderByIdAsc(CONTRAT, ECHEANCE,
                LocalDate.of(2026, 12, 9))).thenReturn(Optional.empty());
        when(factures.reserverIdentifiant()).thenReturn(42L);
        Document pdf = document(77, true);
        when(documentsGeneres.generer(any())).thenReturn(pdf);
        Facture enregistree = facture(42, null);
        when(factures.findById(42L)).thenReturn(Optional.of(enregistree));

        FactureGenereeResponse reponse = service.generer(NUMERO, new DemandeFactureRequest(Periode.ECHEANCE, ECHEANCE));

        // RG4 : FAC-AAAA-NNNNNN. 18,50 € TTC à 9 % : 16,97 € HT + 1,53 € de TSCA.
        verify(factures).verrouillerContrat(CONTRAT);
        verify(factures).inserer(42L, "FAC-2026-000042", CONTRAT, ECHEANCE, LocalDate.of(2026, 12, 9),
                new BigDecimal("16.97"), new BigDecimal("1.53"), new BigDecimal("18.50"), HORLOGE.instant());
        ArgumentCaptor<DemandeDocument> demande = ArgumentCaptor.forClass(DemandeDocument.class);
        verify(documentsGeneres).generer(demande.capture());
        assertThat(demande.getValue().reference()).isEqualTo("FAC-2026-000042");
        assertThat(demande.getValue().contratId()).isEqualTo(CONTRAT);
        assertThat(enregistree.getDocumentId()).isEqualTo(77L);
        assertThat(reponse.existante()).isFalse();
        assertThat(reponse.disponible()).isTrue();
        assertThat(reponse.facture().documentId()).isEqualTo(77L);
    }

    @Test
    void ac2GenereLaFactureDUneAnneeDeCotisation() {
        contratActif("ACTIF", null);
        when(factures.findFirstByContratIdAndPeriodeDebutAndPeriodeFinOrderByIdAsc(CONTRAT, EFFET,
                LocalDate.of(2027, 2, 28))).thenReturn(Optional.empty());
        when(factures.reserverIdentifiant()).thenReturn(43L);
        when(documentsGeneres.generer(any())).thenReturn(document(78, false));
        when(factures.findById(43L)).thenReturn(Optional.of(facture(43, null)));

        FactureGenereeResponse reponse = service.generer(NUMERO, new DemandeFactureRequest(Periode.ANNEE, EFFET));

        verify(factures).inserer(eq(43L), eq("FAC-2026-000043"), eq(CONTRAT), eq(EFFET), eq(LocalDate.of(2027, 2, 28)),
                any(), any(), eq(new BigDecimal("37.00")), any());
        // PDF pas encore produit : la reprise de T-03 le stockera.
        assertThat(reponse.disponible()).isFalse();
        assertThat(reponse.facture().documentId()).isNull();
    }

    @Test
    void ac2UneFactureExistanteEstRenvoyeeSansNouveauNumero() {
        contratActif("ACTIF", null);
        when(factures.findFirstByContratIdAndPeriodeDebutAndPeriodeFinOrderByIdAsc(CONTRAT, ECHEANCE,
                LocalDate.of(2026, 12, 9))).thenReturn(Optional.of(facture(7, 70L)));
        when(documents.findAllById(List.of(70L))).thenReturn(List.of(document(70, true)));

        FactureGenereeResponse reponse = service.generer(NUMERO, new DemandeFactureRequest(Periode.ECHEANCE, ECHEANCE));

        assertThat(reponse.existante()).isTrue();
        assertThat(reponse.disponible()).isTrue();
        assertThat(reponse.facture().numero()).isEqualTo("FAC-2026-000007");
        verify(factures, never()).reserverIdentifiant();
        verifyNoInteractions(documentsGeneres);
    }

    @Test
    void ac2FactureExistanteSansPdfPasEncoreDisponible() {
        contratActif("ACTIF", null);
        when(factures.findFirstByContratIdAndPeriodeDebutAndPeriodeFinOrderByIdAsc(CONTRAT, ECHEANCE,
                LocalDate.of(2026, 12, 9))).thenReturn(Optional.of(facture(7, null)));

        assertThat(service.generer(NUMERO, new DemandeFactureRequest(Periode.ECHEANCE, ECHEANCE)).disponible())
                .isFalse();
    }

    @Test
    void pasDeFacturePourUnContratEnAttenteDeSignature() {
        when(contrats.parNumero(NUMERO)).thenReturn(Optional.of(contrat(CONTRAT, NUMERO, "EN_ATTENTE_DE_SIGNATURE",
                null)));

        assertThatThrownBy(() -> service.generer(NUMERO, new DemandeFactureRequest(Periode.ECHEANCE, ECHEANCE)))
                .isInstanceOf(FactureImpossibleException.class);
        verifyNoInteractions(factures, documentsGeneres);
    }

    @Test
    void contratResiliePasDeFactureApresSaDateDeFin() {
        contratActif("RESILIE", LocalDate.of(2026, 10, 31));

        assertThatThrownBy(() -> service.generer(NUMERO, new DemandeFactureRequest(Periode.ECHEANCE, ECHEANCE)))
                .isInstanceOf(FactureImpossibleException.class);
        verify(factures, never()).reserverIdentifiant();
    }

    @Test
    void contratRenonceSansDateDeFinLimiteAAujourdhui() {
        contratActif("RENONCE", null);
        when(factures.findFirstByContratIdAndPeriodeDebutAndPeriodeFinOrderByIdAsc(eq(CONTRAT), any(), any()))
                .thenReturn(Optional.of(facture(7, null)));

        assertThat(service.generer(NUMERO, new DemandeFactureRequest(Periode.ECHEANCE, ECHEANCE)).existante())
                .isTrue();
    }

    @Test
    void dateQuiNEstNiUneEcheanceNiUneAnnee404() {
        contratActif("ACTIF", null);

        assertThatThrownBy(() -> service.generer(NUMERO,
                new DemandeFactureRequest(Periode.ECHEANCE, LocalDate.of(2026, 11, 11))))
                .isInstanceOf(PeriodeFacturationInconnueException.class);
        assertThatThrownBy(() -> service.generer(NUMERO,
                new DemandeFactureRequest(Periode.ANNEE, LocalDate.of(2025, 3, 1))))
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
}
