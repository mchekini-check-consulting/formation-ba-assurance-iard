package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.DemandeTarif;
import net.checkconsulting.assuranceiard.api.dto.GenerationContratRequest;
import net.checkconsulting.assuranceiard.api.dto.LogementDevis;
import net.checkconsulting.assuranceiard.api.dto.TarifRequest;
import net.checkconsulting.assuranceiard.api.dto.TarifResponse;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.exception.MoteurDonneesInvalidesException;
import net.checkconsulting.assuranceiard.api.exception.MoteurTarifInconnuException;
import net.checkconsulting.assuranceiard.api.exception.TarifDevisIncoherentException;
import net.checkconsulting.assuranceiard.api.mapper.DevisEnregistreMapperImpl;
import net.checkconsulting.assuranceiard.api.mapper.TarifMoteurMapper;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import net.checkconsulting.assuranceiard.api.model.TarifDevis;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import net.checkconsulting.assuranceiard.api.repository.TarifDevisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TarifDevisServiceTest {

    private static final UUID REFERENCE = UUID.randomUUID();
    private static final UUID ID_TARIF = UUID.randomUUID();
    private static final Instant MAINTENANT = Instant.EPOCH;
    private static final String CODE_POSTAL = "69003";
    private static final String TYPE = "appartement";
    private static final int SURFACE = 45;
    private static final int FRANCHISE = 300;
    private static final TarifResponse.Detail DETAIL = new TarifResponse.Detail(
            "CONFORT", null, List.of(), null, BigDecimal.TEN, BigDecimal.ONE);

    @Mock
    private MoteurTarifaireService moteur;

    @Mock
    private TarifDevisRepository tarifs;

    @Mock
    private RecommandationDevisRepository recommandations;

    private final RecommandationDevis recommandation = new RecommandationDevis();

    private TarifDevisService service;

    @BeforeEach
    void preparer() {
        service = new TarifDevisService(moteur, tarifs, recommandations, new TarifMoteurMapper(),
                new DevisEnregistreMapperImpl(), Clock.fixed(MAINTENANT, ZoneOffset.UTC));
        recommandation.setValeurContenu(15_000);
        recommandation.setRemboursement("neuf");
        recommandation.setPriorite("equilibre");
        when(recommandations.findByReference(REFERENCE)).thenReturn(Optional.of(recommandation));
    }

    private LogementDevis logement() {
        return new LogementDevis("locataire", "principale", TYPE, "12 rue de la Paix", CODE_POSTAL, "Lyon",
                String.valueOf(SURFACE), "2", "non-assure", "", "", "rdc", null, "", "", "0");
    }

    private DemandeTarif demande(String formule) {
        return new DemandeTarif(logement(), null, formule, List.of("objets-valeur"), String.valueOf(FRANCHISE));
    }

    private TarifResponse tarif(boolean tarifable) {
        return new TarifResponse(ID_TARIF, "v", tarifable, null, null, null, tarifable ? DETAIL : null,
                null, null, null, null, null, null);
    }

    private TarifDevis ligneRetenue() {
        TarifDevis ligne = new TarifDevis();
        ligne.setIdTarif(ID_TARIF);
        ligne.setCodePostal(CODE_POSTAL);
        ligne.setTypeLogement(TYPE);
        ligne.setSurface(BigDecimal.valueOf(SURFACE));
        ligne.setFranchise(FRANCHISE);
        return ligne;
    }

    private GenerationContratRequest contrat(String codePostal, String type, int surface, int franchise) {
        return new GenerationContratRequest(REFERENCE, "adresse", codePostal, "ville", type, surface, franchise, false);
    }

    @Test
    void estimerAppelleLeMoteurSansRienRetenir() {
        when(moteur.calculer(any())).thenReturn(tarif(true));

        assertThat(service.estimer(demande(null)).idTarif()).isEqualTo(ID_TARIF);

        verify(tarifs, never()).save(any());
    }

    @Test
    void retientLeTarifAvecLesChoixDuClient() {
        when(moteur.calculer(any())).thenReturn(tarif(true));
        when(tarifs.findByReferenceDevis(REFERENCE)).thenReturn(Optional.empty());

        service.tarifer(REFERENCE, demande("serenite"));

        ArgumentCaptor<TarifDevis> capture = ArgumentCaptor.forClass(TarifDevis.class);
        verify(tarifs).save(capture.capture());
        TarifDevis retenu = capture.getValue();
        assertThat(retenu.getReferenceDevis()).isEqualTo(REFERENCE);
        assertThat(retenu.getIdTarif()).isEqualTo(ID_TARIF);
        assertThat(retenu.getFormule()).isEqualTo("serenite");
        assertThat(retenu.getOptions()).containsExactly("objets-valeur");
        assertThat(retenu.getDateCreation()).isEqualTo(MAINTENANT);
    }

    @Test
    void lesBesoinsViennentDeLaRecommandationEnregistreePasDuNavigateur() {
        when(moteur.calculer(any())).thenReturn(tarif(true));
        when(tarifs.findByReferenceDevis(REFERENCE)).thenReturn(Optional.empty());

        service.tarifer(REFERENCE, demande("confort"));

        ArgumentCaptor<TarifRequest> capture = ArgumentCaptor.forClass(TarifRequest.class);
        verify(moteur).calculer(capture.capture());
        assertThat(capture.getValue().besoins().valeurContenu()).isEqualByComparingTo("15000");
    }

    @Test
    void unNouveauCalculRemplaceLePrecedent() {
        TarifDevis existant = new TarifDevis();
        when(moteur.calculer(any())).thenReturn(tarif(true));
        when(tarifs.findByReferenceDevis(REFERENCE)).thenReturn(Optional.of(existant));

        service.tarifer(REFERENCE, demande("confort"));

        verify(tarifs).save(existant);
        assertThat(existant.getIdTarif()).isEqualTo(ID_TARIF);
    }

    @Test
    void unRefusRetireLeTarifRetenu() {
        TarifDevis existant = new TarifDevis();
        when(moteur.calculer(any())).thenReturn(tarif(false));
        when(tarifs.findByReferenceDevis(REFERENCE)).thenReturn(Optional.of(existant));

        service.tarifer(REFERENCE, demande("confort"));

        verify(tarifs).delete(existant);
        verify(tarifs, never()).save(any());
    }

    @Test
    void unDevisInconnuEstRefuse() {
        UUID inconnue = UUID.randomUUID();
        when(recommandations.findByReference(inconnue)).thenReturn(Optional.empty());
        DemandeTarif demande = demande("confort");

        assertThatThrownBy(() -> service.tarifer(inconnue, demande)).isInstanceOf(DevisInconnuException.class);
        verify(moteur, never()).calculer(any());
    }

    @Test
    void sansFormuleIlNYaPasDeTotalARetenir() {
        DemandeTarif demande = demande(null);

        assertThatThrownBy(() -> service.tarifer(REFERENCE, demande)).isInstanceOf(MoteurDonneesInvalidesException.class);
        verify(moteur, never()).calculer(any());
    }

    @Test
    void uneRepriseRelitLeTarifRetenu() {
        when(tarifs.findByReferenceDevis(REFERENCE)).thenReturn(Optional.of(ligneRetenue()));
        when(moteur.reprendre(ID_TARIF)).thenReturn(tarif(true));

        assertThat(service.tarifRetenu(REFERENCE).idTarif()).isEqualTo(ID_TARIF);
    }

    @Test
    void uneRepriseSansTarifRetenuEstSignalee() {
        when(tarifs.findByReferenceDevis(REFERENCE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.tarifRetenu(REFERENCE)).isInstanceOf(MoteurTarifInconnuException.class);
    }

    @Test
    void donneLePrixDUnDevisEnregistre() {
        when(moteur.calculer(any())).thenReturn(tarif(true));

        assertThat(service.prixMensuel(logement(), recommandation, "confort", List.of(), "300"))
                .isEqualByComparingTo(DETAIL.totalMensuel());
    }

    @Test
    void unProfilRefuseNaPasDePrix() {
        when(moteur.calculer(any())).thenReturn(tarif(false));
        LogementDevis logement = logement();
        List<String> sansOption = List.of();

        assertThatThrownBy(() -> service.prixMensuel(logement, recommandation, "confort", sansOption, "300"))
                .isInstanceOf(TarifDevisIncoherentException.class);
    }

    @Test
    void laCotisationDuContratVientDuMoteur() {
        when(tarifs.findByReferenceDevis(REFERENCE)).thenReturn(Optional.of(ligneRetenue()));
        when(moteur.reprendre(ID_TARIF)).thenReturn(tarif(true));

        assertThat(service.cotisationDuContrat(contrat(CODE_POSTAL, TYPE, SURFACE, FRANCHISE))).isEqualTo(DETAIL);
    }

    @Test
    void sansTarifRetenuLeContratEstRefuse() {
        when(tarifs.findByReferenceDevis(REFERENCE)).thenReturn(Optional.empty());
        GenerationContratRequest contrat = contrat(CODE_POSTAL, TYPE, SURFACE, FRANCHISE);

        assertThatThrownBy(() -> service.cotisationDuContrat(contrat)).isInstanceOf(TarifDevisIncoherentException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "75001, appartement, 45, 300",
        "69003, maison, 45, 300",
        "69003, appartement, 46, 300",
        "69003, appartement, 45, 500"
    })
    void unLogementDifferentDuTarifEstRefuse(String codePostal, String type, int surface, int franchise) {
        when(tarifs.findByReferenceDevis(REFERENCE)).thenReturn(Optional.of(ligneRetenue()));
        GenerationContratRequest contrat = contrat(codePostal, type, surface, franchise);

        assertThatThrownBy(() -> service.cotisationDuContrat(contrat)).isInstanceOf(TarifDevisIncoherentException.class);
        verify(moteur, never()).reprendre(any());
    }

    private static net.checkconsulting.assuranceiard.api.dto.DonneesContrat contratHabitation() {
        return new net.checkconsulting.assuranceiard.api.dto.DonneesContrat(1L, "AE-RECETTE-H01", "HABITATION",
                "confort", null,
                new net.checkconsulting.assuranceiard.api.dto.DonneesContrat.Logement("appartement", 55,
                        "3 rue de la Recette", "33000", "Bordeaux"),
                null, null,
                new net.checkconsulting.assuranceiard.api.dto.DonneesContrat.Tarif(new BigDecimal("300.00"),
                        new BigDecimal("18.50"), new BigDecimal("222.00"), null, null),
                null, null, List.of());
    }

    @Test
    void us23PrixDesOptionsHabitationAvecLaGrilleDuSprint1() {
        TarifResponse.Detail detail = new TarifResponse.Detail("CONFORT", new BigDecimal("13.19"), List.of(
                new TarifResponse.PrixOption("OBJETS_VALEUR", new BigDecimal("2.50")),
                new TarifResponse.PrixOption("ASSISTANCE", new BigDecimal("1.50")),
                new TarifResponse.PrixOption("PROTECTION_JURIDIQUE", new BigDecimal("2.90"))),
                BigDecimal.ZERO, new BigDecimal("20.09"), new BigDecimal("241.08"));
        when(moteur.calculer(any())).thenReturn(new TarifResponse(ID_TARIF, "2026.1", true, null, null, null, detail,
                null, null, null, null, null, null));

        assertThat(service.prixDesOptions(contratHabitation())).containsExactly(
                java.util.Map.entry("objets-valeur", new BigDecimal("2.50")),
                java.util.Map.entry("assistance-depannage", new BigDecimal("1.50")),
                java.util.Map.entry("protection-juridique", new BigDecimal("2.90")));
        ArgumentCaptor<TarifRequest> requete = ArgumentCaptor.forClass(TarifRequest.class);
        verify(moteur).calculer(requete.capture());
        // Profil neutre (le contrat ne garde pas tout le profil du devis), formule et franchise du contrat.
        assertThat(requete.getValue().formule()).isEqualTo("CONFORT");
        assertThat(requete.getValue().franchise()).isEqualTo(300);
        assertThat(requete.getValue().pieces()).isEqualTo(2);
        assertThat(requete.getValue().options()).containsExactly("OBJETS_VALEUR", "ASSISTANCE", "PROTECTION_JURIDIQUE");
        // Recette US-23 (KO « champs invalides ») : le moteur exige l'étage d'un appartement et les dépendances d'une
        // maison (ChampsConditionnelsValidator) ; le profil neutre les renseigne (coefficient 1).
        assertThat(requete.getValue().typeLogement()).isEqualTo("APPARTEMENT");
        assertThat(requete.getValue().etage()).isEqualTo("INTERMEDIAIRE");
        assertThat(requete.getValue().dependances()).isEqualTo("AUCUNE");
    }

    @Test
    void us23RecetteUnRefusDuMoteurNAfficheJamaisChampsInvalidesAuClient() {
        when(moteur.calculer(any())).thenThrow(new MoteurDonneesInvalidesException(
                List.of(new net.checkconsulting.assuranceiard.api.dto.ErreurChampMoteur("etage", "obligatoire"))));

        assertThatThrownBy(() -> service.prixDesOptions(contratHabitation()))
                .isInstanceOf(net.checkconsulting.assuranceiard.api.exception.MoteurIndisponibleException.class)
                .hasMessageNotContaining("champs invalides");
    }

    @Test
    void us23PrixDesOptionsHabitationIndisponiblesSiLeMoteurNeTarifePas() {
        when(moteur.calculer(any())).thenReturn(tarif(false));

        assertThatThrownBy(() -> service.prixDesOptions(contratHabitation()))
                .isInstanceOf(net.checkconsulting.assuranceiard.api.exception.MoteurIndisponibleException.class);
    }
}
