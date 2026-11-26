package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.ContratResumeResponse;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.dto.FicheContratResponse;
import net.checkconsulting.assuranceiard.api.dto.TableauDeBordResponse;
import net.checkconsulting.assuranceiard.api.exception.ContratInconnuException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.DevisAuto;
import net.checkconsulting.assuranceiard.api.model.DevisEnregistre;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Facture;
import net.checkconsulting.assuranceiard.api.model.MandatSepa;
import net.checkconsulting.assuranceiard.api.model.Profil;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.model.Sinistre;
import net.checkconsulting.assuranceiard.api.model.Vehicule;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisAutoRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisEnregistreRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.repository.FactureRepository;
import net.checkconsulting.assuranceiard.api.repository.PermissionRepository;
import net.checkconsulting.assuranceiard.api.repository.SinistreRepository;
import net.checkconsulting.assuranceiard.api.repository.VehiculeRepository;
import net.checkconsulting.assuranceiard.api.service.avenant.HistoriqueAvenants;
import net.checkconsulting.assuranceiard.api.service.document.DocumentsGeneresService;
import net.checkconsulting.assuranceiard.api.service.document.GabaritPdf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EspaceClientServiceTest {

    private static final String JETON = "Bearer jeton";
    private static final UUID REFERENCE = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 24);

    private final SessionService sessions = mock(SessionService.class);
    private final PermissionRepository permissions = mock(PermissionRepository.class);
    private final ContratRepository contrats = mock(ContratRepository.class);
    private final DevisEnregistreRepository devis = mock(DevisEnregistreRepository.class);
    private final DevisAutoRepository devisAuto = mock(DevisAutoRepository.class);
    private final VehiculeRepository vehicules = mock(VehiculeRepository.class);
    private final FactureRepository factures = mock(FactureRepository.class);
    private final SinistreRepository sinistres = mock(SinistreRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final DonneesContratRepository donnees = mock(DonneesContratRepository.class);
    private final Clock horloge = Clock.fixed(Instant.parse("2026-11-24T09:00:00Z"), ZoneOffset.UTC);

    private final DocumentsGeneresService documentsGeneres = mock(DocumentsGeneresService.class);
    private final HistoriqueAvenants historiqueAvenants = mock(HistoriqueAvenants.class);
    private final EspaceClientService service = new EspaceClientService(sessions, permissions, contrats, devis,
            devisAuto, vehicules, factures, sinistres, documents, donnees, documentsGeneres, historiqueAvenants,
            horloge);

    private Compte compte;

    @BeforeEach
    void connecter() {
        compte = new Compte();
        compte.setId(7L);
        compte.setPrenom("Camille");
        compte.setNom("Durand");
        compte.setProfil(Profil.ASSURE);
        when(sessions.compteConnecte(JETON)).thenReturn(compte);
        autoriser(Rubrique.CONTRATS, Rubrique.DEVIS, Rubrique.SINISTRES);
    }

    private void autoriser(Rubrique... rubriques) {
        for (Rubrique rubrique : Rubrique.values()) {
            when(permissions.existsByProfilAndRubriqueAndDroitAndValeurTrue(any(), eq(rubrique), eq(Droit.CONSULTER)))
                    .thenReturn(List.of(rubriques).contains(rubrique));
        }
    }

    private static Contrat habitation() {
        Contrat contrat = new Contrat();
        contrat.setId(1L);
        contrat.setNumero("AE-RECETTE-1");
        contrat.setProduit(Contrat.HABITATION);
        contrat.setStatut("ACTIF");
        contrat.setFormule("confort");
        contrat.setAdresseLogement("1 rue de la Recette");
        contrat.setCodePostal("75011");
        contrat.setVille("Paris");
        contrat.setTypeLogement("appartement");
        MandatSepa mandat = new MandatSepa();
        mandat.setIbanMasque("•••• 0189");
        contrat.setMandatSepa(mandat);
        contrat.setFranchise(new BigDecimal("300.00"));
        contrat.setCotisationMensuelle(new BigDecimal("18.50"));
        contrat.setCotisationAnnuelle(new BigDecimal("222.00"));
        contrat.setDateEffet(LocalDate.of(2026, 1, 10));
        DossierSouscription dossier = new DossierSouscription();
        dossier.setReferenceDevis(REFERENCE);
        contrat.setDossier(dossier);
        return contrat;
    }

    private static Contrat auto(long id, String numero, String statut) {
        Contrat contrat = new Contrat();
        contrat.setId(id);
        contrat.setNumero(numero);
        contrat.setProduit(Contrat.AUTO);
        contrat.setStatut(statut);
        contrat.setFormule("TR");
        contrat.setFranchise(new BigDecimal("500.00"));
        contrat.setCotisationMensuelle(new BigDecimal("45.00"));
        contrat.setCotisationAnnuelle(new BigDecimal("540.00"));
        contrat.setDateEffet(LocalDate.of(2026, 9, 25));
        contrat.setVehicule(vehicule("AB-003-CD"));
        return contrat;
    }

    private static Vehicule vehicule(String immatriculation) {
        Vehicule vehicule = new Vehicule();
        vehicule.setId(30L);
        vehicule.setMarque("Renault");
        vehicule.setModele("Clio");
        vehicule.setImmatriculation(immatriculation);
        return vehicule;
    }

    @Test
    void mesContratsHabitationEtAutoAvecBienAssureEtEcheance() {
        Contrat resilie = auto(4L, "AE-RECETTE-A09", "RESILIE");
        resilie.setDateFin(LocalDate.of(2026, 11, 19));
        when(contrats.duClient(compte)).thenReturn(List.of(habitation(), auto(3L, "AE-RECETTE-A03", "ACTIF"), resilie));

        assertThat(service.contrats(JETON)).containsExactly(
                new ContratResumeResponse(REFERENCE, "AE-RECETTE-1", "HABITATION", "ACTIF", "confort",
                        "1 rue de la Recette, 75011 Paris", "appartement", "Paris", new BigDecimal("18.50"),
                        LocalDate.of(2026, 1, 10), LocalDate.of(2027, 1, 10), null, "•••• 0189"),
                new ContratResumeResponse(null, "AE-RECETTE-A03", "AUTO", "ACTIF", "TR", "Renault Clio · AB-003-CD", null,
                        null, new BigDecimal("45.00"), LocalDate.of(2026, 9, 25), LocalDate.of(2027, 9, 25), null,
                        null),
                new ContratResumeResponse(null, "AE-RECETTE-A09", "AUTO", "RESILIE", "TR", "Renault Clio · AB-003-CD", null,
                        null, new BigDecimal("45.00"), LocalDate.of(2026, 9, 25), null, LocalDate.of(2026, 11, 19),
                        null));
    }

    @Test
    void unBienSansInformationsResteVide() {
        Contrat sansVehicule = auto(5L, "AE-X", "ACTIF");
        sansVehicule.setVehicule(null);
        Contrat sansAdresse = habitation();
        sansAdresse.setAdresseLogement(null);
        sansAdresse.setCodePostal(null);
        sansAdresse.setVille(" ");
        Vehicule nonImmatricule = vehicule(null);

        assertThat(EspaceClientService.bienAssure(sansVehicule)).isNull();
        assertThat(EspaceClientService.bienAssure(sansAdresse)).isNull();
        assertThat(EspaceClientService.vehicule(nonImmatricule)).isEqualTo("Renault Clio");
    }

    @Test
    void ac1TableauDeBordComplet() {
        when(contrats.duClient(compte)).thenReturn(List.of(habitation(), auto(3L, "AE-RECETTE-A03", "ACTIF")));
        DevisEnregistre devisHabitation = unDevis(10L, "HABITATION", "confort");
        devisHabitation.setAdresse("3 rue des Devis");
        devisHabitation.setCodePostal("33000");
        devisHabitation.setVille("Bordeaux");
        DevisEnregistre devisVoiture = unDevis(11L, "AUTO", "TR");
        when(devis.enCours(compte, AUJOURDHUI)).thenReturn(List.of(devisHabitation, devisVoiture));
        DevisAuto partieAuto = new DevisAuto();
        partieAuto.setDevisId(11L);
        partieAuto.setVehiculeId(30L);
        when(devisAuto.findByDevisIdIn(anyCollection())).thenReturn(List.of(partieAuto));
        when(vehicules.findAllById(List.of(30L))).thenReturn(List.of(vehicule("EF-003-GH")));
        Facture stockee = uneFacture("FAC-1", 1L, 100L);
        Facture pasEncoreStockee = uneFacture("FAC-2", 3L, 101L);
        Facture sansDocument = uneFacture("FAC-3", 3L, null);
        when(factures.findTop3ByContratIdInOrderByPeriodeFinDescIdDesc(anyCollection()))
                .thenReturn(List.of(stockee, pasEncoreStockee, sansDocument));
        when(documents.findAllById(List.of(100L, 101L))).thenReturn(List.of(unDocument(100L, true),
                unDocument(101L, false)));
        Sinistre declare = new Sinistre();
        declare.setNumero("SIN-1");
        declare.setContratId(1L);
        declare.setType("DEGAT_EAUX");
        declare.setDateSurvenance(LocalDate.of(2026, 11, 14));
        when(sinistres.findByContratIdInAndStatutOrderByDateSurvenanceDescIdDesc(anyCollection(), eq("DECLARE")))
                .thenReturn(List.of(declare));

        TableauDeBordResponse tableau = service.tableauDeBord(JETON);

        assertThat(tableau.prenom()).isEqualTo("Camille");
        assertThat(tableau.nom()).isEqualTo("Durand");
        assertThat(tableau.contrats()).extracting(ContratResumeResponse::numero)
                .containsExactly("AE-RECETTE-1", "AE-RECETTE-A03");
        assertThat(tableau.devisVisibles()).isTrue();
        assertThat(tableau.devisEnCours()).containsExactly(
                new TableauDeBordResponse.Devis("HABITATION", "3 rue des Devis, 33000 Bordeaux", "confort",
                        new BigDecimal("54.90"), LocalDate.of(2026, 12, 24)),
                new TableauDeBordResponse.Devis("AUTO", "Renault Clio · EF-003-GH", "TR", new BigDecimal("54.90"),
                        LocalDate.of(2026, 12, 24)));
        assertThat(tableau.dernieresFactures()).extracting(TableauDeBordResponse.Facture::numeroContrat,
                        TableauDeBordResponse.Facture::documentId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("AE-RECETTE-1", 100L),
                        org.assertj.core.groups.Tuple.tuple("AE-RECETTE-A03", null),
                        org.assertj.core.groups.Tuple.tuple("AE-RECETTE-A03", null));
        assertThat(tableau.sinistresVisibles()).isTrue();
        assertThat(tableau.sinistres()).containsExactly(new TableauDeBordResponse.Sinistre("SIN-1", "AE-RECETTE-1",
                "DEGAT_EAUX", LocalDate.of(2026, 11, 14), null));
    }

    @Test
    void ac6UnProspectVoitSonPrenomEtSesDevisSansAucunContrat() {
        compte.setProfil(Profil.PROSPECT);
        autoriser(Rubrique.DEVIS);
        when(devis.enCours(compte, AUJOURDHUI)).thenReturn(List.of());

        TableauDeBordResponse tableau = service.tableauDeBord(JETON);

        assertThat(tableau.prenom()).isEqualTo("Camille");
        assertThat(tableau.contrats()).isEmpty();
        assertThat(tableau.dernieresFactures()).isEmpty();
        assertThat(tableau.devisVisibles()).isTrue();
        assertThat(tableau.sinistresVisibles()).isFalse();
        assertThat(tableau.sinistres()).isEmpty();
        verifyNoInteractions(contrats, factures, sinistres);
    }

    @Test
    void uneRubriqueNonAutoriseeNEstPasLue() {
        autoriser(Rubrique.CONTRATS);
        when(contrats.duClient(compte)).thenReturn(List.of(habitation()));

        TableauDeBordResponse tableau = service.tableauDeBord(JETON);

        assertThat(tableau.devisVisibles()).isFalse();
        assertThat(tableau.devisEnCours()).isEmpty();
        assertThat(tableau.sinistresVisibles()).isFalse();
        verify(devis, never()).enCours(any(), any());
        verifyNoInteractions(sinistres);
    }

    @Test
    void unDevisAutoSansVehiculeConnuNAPasDeDescription() {
        when(contrats.duClient(compte)).thenReturn(List.of());
        when(devis.enCours(compte, AUJOURDHUI)).thenReturn(List.of(unDevis(11L, "AUTO", "TR")));
        when(devisAuto.findByDevisIdIn(anyCollection())).thenReturn(List.of());

        assertThat(service.tableauDeBord(JETON).devisEnCours()).singleElement()
                .extracting(TableauDeBordResponse.Devis::description).isNull();
    }

    @Test
    void ac3FicheAutoAvecVehiculeGarantiesIbanMasqueEtDocuments() {
        Contrat contrat = auto(3L, "AE-RECETTE-A03", "ACTIF");
        when(contrats.parNumero("AE-RECETTE-A03")).thenReturn(Optional.of(contrat));
        when(donnees.lire(3L)).thenReturn(Optional.of(donnees(contrat, new DonneesContrat.Logement(null, null, null,
                null, null), new DonneesContrat.Vehicule("AB-003-CD", "Renault", "Clio", "V TCe 90"))));
        when(documents.enVigueurDuContrat(eq(3L), anyCollection())).thenReturn(List.of());
        when(documents.enVigueur(0, "CONDITIONS_GENERALES", "AUTO")).thenReturn(Optional.of(unDocument(201L, true,
                "CONDITIONS_GENERALES")));
        Document v2 = unDocument(200L, true, "CONDITIONS_PARTICULIERES");
        v2.setVersion(2);
        v2.setStatut("EN_VIGUEUR");
        v2.setMotif("DEMANDE_CLIENT");
        Document v1 = unDocument(199L, false, "CONDITIONS_PARTICULIERES");
        v1.setVersion(1);
        v1.setStatut("REMPLACE");
        v1.setMotif("SIGNATURE");
        when(documents.versionsDuContrat(3L, "CONDITIONS_PARTICULIERES")).thenReturn(List.of(v2, v1));
        Instant produitLe = Instant.parse("2026-11-24T09:30:00Z");
        when(documentsGeneres.production(v2)).thenReturn(Optional.of(new GabaritPdf.Production(produitLe, "abc")));
        when(documentsGeneres.production(v1)).thenReturn(Optional.empty());
        when(documentsGeneres.contenuAJour(v2)).thenReturn(true);

        FicheContratResponse fiche = service.fiche("AE-RECETTE-A03");

        assertThat(fiche.produit()).isEqualTo("AUTO");
        assertThat(fiche.logement()).isNull();
        assertThat(fiche.vehicule()).isEqualTo(new FicheContratResponse.Vehicule("Renault", "Clio", "V TCe 90",
                "AB-003-CD"));
        assertThat(fiche.garanties()).containsExactly(new FicheContratResponse.Garantie("RC", new BigDecimal("12.00")));
        assertThat(fiche.paiement().ibanMasque()).isEqualTo("•••• 7788");
        assertThat(fiche.dateEcheance()).isEqualTo(LocalDate.of(2027, 9, 25));
        assertThat(fiche.referenceDevis()).isNull();
        // US-21 : les conditions particulières ne sont plus dans « Documents », mais dans « Versions ».
        assertThat(fiche.documents()).extracting(FicheContratResponse.Document::id,
                        FicheContratResponse.Document::libelle)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(201L, "Conditions générales"));
        assertThat(fiche.versionsConditionsParticulieres()).containsExactly(
                new FicheContratResponse.Version(200L, 2, "EN_VIGUEUR", null, "DEMANDE_CLIENT", produitLe, true),
                new FicheContratResponse.Version(199L, 1, "REMPLACE", null, "SIGNATURE", null, false));
        assertThat(fiche.conditionsParticulieresAJour()).isTrue();
    }

    @Test
    void us21SansVersionEnVigueurLesConditionsParticulieresNeSontPasAJour() {
        Contrat contrat = habitation();
        when(contrats.parNumero("AE-RECETTE-1")).thenReturn(Optional.of(contrat));
        when(donnees.lire(1L)).thenReturn(Optional.of(donnees(contrat, new DonneesContrat.Logement("appartement",
                45, "1 rue de la Recette", "75011", "Paris"), new DonneesContrat.Vehicule(null, null, null, null))));
        Document remplacee = unDocument(150L, true, "CONDITIONS_PARTICULIERES");
        remplacee.setStatut("REMPLACE");
        when(documents.versionsDuContrat(1L, "CONDITIONS_PARTICULIERES")).thenReturn(List.of(remplacee));

        FicheContratResponse fiche = service.fiche("AE-RECETTE-1");

        assertThat(fiche.conditionsParticulieresAJour()).isFalse();
        verify(documentsGeneres, never()).contenuAJour(any());
    }

    @Test
    void ac3FicheHabitationSansConditionsGeneralesEncoreCreees() {
        Contrat contrat = habitation();
        when(contrats.parNumero("AE-RECETTE-1")).thenReturn(Optional.of(contrat));
        when(donnees.lire(1L)).thenReturn(Optional.of(donnees(contrat, new DonneesContrat.Logement("appartement",
                45, "1 rue de la Recette", "75011", "Paris"), new DonneesContrat.Vehicule(null, null, null, null))));
        when(documents.enVigueurDuContrat(eq(1L), anyCollection())).thenReturn(List.of());
        when(documents.enVigueur(0, "CONDITIONS_GENERALES", "HABITATION")).thenReturn(Optional.empty());

        FicheContratResponse fiche = service.fiche("AE-RECETTE-1");

        assertThat(fiche.referenceDevis()).isEqualTo(REFERENCE);
        assertThat(fiche.vehicule()).isNull();
        assertThat(fiche.logement()).isEqualTo(new FicheContratResponse.Logement("appartement", 45,
                "1 rue de la Recette", "75011", "Paris"));
        assertThat(fiche.documents()).isEmpty();
    }

    @Test
    void ficheDUnContratInconnu() {
        when(contrats.parNumero("AE-INCONNU")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.fiche("AE-INCONNU")).isInstanceOf(ContratInconnuException.class);
    }

    @Test
    void us23MesContratsListeLesAvenantsAcceptesAvecLeurPdfPourMesDocuments() {
        Contrat auto = auto(3L, "AE-RECETTE-A03", "ACTIF");
        when(contrats.duClient(compte)).thenReturn(List.of(auto, habitation()));
        net.checkconsulting.assuranceiard.api.dto.AvenantResponse accepte =
                new net.checkconsulting.assuranceiard.api.dto.AvenantResponse("AV-2026-000009", 2, "ACCEPTE",
                        LocalDate.of(2026, 11, 27), new BigDecimal("47.50"), new BigDecimal("48.50"), List.of(),
                        List.of(), Instant.parse("2026-11-26T10:00:00Z"), 70L);
        net.checkconsulting.assuranceiard.api.dto.AvenantResponse abandonne =
                new net.checkconsulting.assuranceiard.api.dto.AvenantResponse("AV-2026-000004", 1, "ABANDONNE",
                        LocalDate.of(2026, 11, 20), new BigDecimal("45.00"), new BigDecimal("47.50"), List.of(),
                        List.of(), null, null);
        when(historiqueAvenants.duContrat(3L)).thenReturn(List.of(accepte, abandonne));
        when(historiqueAvenants.duContrat(1L)).thenReturn(List.of());

        List<ContratResumeResponse> resultat = service.contrats(JETON);

        assertThat(resultat.getFirst().avenants()).containsExactly(accepte);
        assertThat(resultat.get(1).avenants()).isEmpty();
    }

    @Test
    void lesDocumentsDeLaFicheNeComprennentNiFacturesNiAvenantsNiConditionsParticulieres() {
        assertThat(EspaceClientService.DOCUMENTS_FICHE).extracting(Enum::name)
                .doesNotContain("FACTURE", "AVENANT", "DEVIS", "DECLARATION_SINISTRE", "ECHEANCIER",
                        "CONDITIONS_PARTICULIERES");
    }

    private static DonneesContrat donnees(Contrat contrat, DonneesContrat.Logement logement,
            DonneesContrat.Vehicule vehicule) {
        return new DonneesContrat(contrat.getId(), contrat.getNumero(), contrat.getProduit(), contrat.getFormule(),
                new DonneesContrat.Souscripteur("madame", "Camille", "Durand"), logement, vehicule,
                new DonneesContrat.Conducteur(new BigDecimal("0.68"), true),
                new DonneesContrat.Tarif(contrat.getFranchise(), contrat.getCotisationMensuelle(),
                        contrat.getCotisationAnnuelle(), contrat.getDateEffet(), null),
                new DonneesContrat.Paiement("MENSUEL", 5, "•••• 7788", "AE-RUM-RECETTE-A"), null,
                List.of(new DonneesContrat.Option("RC", new BigDecimal("12.00"))));
    }

    private static DevisEnregistre unDevis(long id, String produit, String formule) {
        DevisEnregistre devis = new DevisEnregistre();
        devis.setId(id);
        devis.setProduit(produit);
        devis.setFormule(formule);
        devis.setPrixMensuel(new BigDecimal("54.90"));
        devis.setDateFinValidite(LocalDate.of(2026, 12, 24));
        return devis;
    }

    private static Facture uneFacture(String numero, long contrat, Long document) {
        Facture facture = new Facture();
        facture.setNumero(numero);
        facture.setContratId(contrat);
        facture.setDocumentId(document);
        facture.setPeriodeDebut(LocalDate.of(2026, 10, 1));
        facture.setPeriodeFin(LocalDate.of(2026, 10, 31));
        facture.setMontantTtc(new BigDecimal("18.50"));
        return facture;
    }

    private static Document unDocument(long id, boolean stocke) {
        return unDocument(id, stocke, "FACTURE");
    }

    private static Document unDocument(long id, boolean stocke, String type) {
        Document document = new Document();
        document.setId(id);
        document.setType(type);
        document.setStocke(stocke);
        return document;
    }
}
