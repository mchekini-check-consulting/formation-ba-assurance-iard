package net.checkconsulting.assuranceiard.api.service.sinistre;

import static net.checkconsulting.assuranceiard.api.service.sinistre.FixturesSinistre.AUJOURDHUI;
import static net.checkconsulting.assuranceiard.api.service.sinistre.FixturesSinistre.MAINTENANT;
import static net.checkconsulting.assuranceiard.api.service.sinistre.FixturesSinistre.brouillon;
import static net.checkconsulting.assuranceiard.api.service.sinistre.FixturesSinistre.camille;
import static net.checkconsulting.assuranceiard.api.service.sinistre.FixturesSinistre.contrat;
import static net.checkconsulting.assuranceiard.api.service.sinistre.FixturesSinistre.option;
import static net.checkconsulting.assuranceiard.api.service.sinistre.FixturesSinistre.present;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.ContexteDeclarationResponse;
import net.checkconsulting.assuranceiard.api.dto.CreationSinistreRequest;
import net.checkconsulting.assuranceiard.api.dto.DeclarationSinistreResponse;
import net.checkconsulting.assuranceiard.api.dto.MiseAJourSinistreRequest;
import net.checkconsulting.assuranceiard.api.dto.SinistreResumeResponse;
import net.checkconsulting.assuranceiard.api.dto.TypesSinistreResponse;
import net.checkconsulting.assuranceiard.api.exception.DeclarationDejaEnvoyeeException;
import net.checkconsulting.assuranceiard.api.exception.DeclarationImpossibleException;
import net.checkconsulting.assuranceiard.api.exception.DeclarationInvalideException;
import net.checkconsulting.assuranceiard.api.exception.SinistreInconnuException;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.Sinistre;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.SinistreRepository;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** US-24 : parcours de déclaration (AC1 à AC8, AC11 à AC13, RG4, RG6, RG9, RG11). */
class DeclarationSinistreServiceTest {

    private static final String JETON = "Bearer jeton";

    private final FixturesSinistre fixtures = new FixturesSinistre();
    private final SessionService sessions = mock(SessionService.class);
    private final ContratRepository contrats = mock(ContratRepository.class);
    private final SinistreRepository sinistres = mock(SinistreRepository.class);
    private final DeclarationSinistreService service = new DeclarationSinistreService(sessions, contrats, sinistres,
            fixtures.referentiel, fixtures.vue, fixtures.json, fixtures.proprietes);

    /** A03 (Tous risques, actif) et A04 (Tiers, actif, justificatifs attendus), comme Camille. */
    private final Contrat a03 = contrat(3L, "AE-RECETTE-A03", "ACTIF", "TR");
    private final Contrat a04 = contrat(4L, "AE-RECETTE-A04", "ACTIF_JUSTIFICATIFS_ATTENDUS", "TIERS");

    @BeforeEach
    void preparer() {
        when(sessions.compteConnecte(JETON)).thenReturn(camille());
        when(contrats.parNumero("AE-RECETTE-A03")).thenReturn(present(a03));
        when(contrats.parNumero("AE-RECETTE-A04")).thenReturn(present(a04));
        when(contrats.findById(3L)).thenReturn(present(a03));
        when(contrats.findById(4L)).thenReturn(present(a04));
        when(sinistres.save(any(Sinistre.class))).thenAnswer(appel -> {
            Sinistre sinistre = appel.getArgument(0);
            if (sinistre.getId() == null) {
                sinistre.setId(42L);
                sinistre.setBrouillonExpireLe(MAINTENANT.plusSeconds(30L * 86_400));
            }
            return sinistre;
        });
    }

    @Test
    void ac1SeulsLesContratsOuvertsALaDeclarationSontProposes() {
        Contrat enAttente = contrat(1L, "AE-RECETTE-A01", "EN_ATTENTE_DE_SIGNATURE", "TIERS");
        Contrat resilieRecent = contrat(9L, "AE-RECETTE-A09", "RESILIE", "TR");
        resilieRecent.setDateFin(AUJOURDHUI.minusDays(5));
        Contrat resilieAncien = contrat(11L, "AE-ANCIEN", "RESILIE", "TR");
        resilieAncien.setDateFin(AUJOURDHUI.minusYears(3));
        Contrat renonce = contrat(10L, "AE-RECETTE-A10", "RENONCE", "TIERS");
        when(contrats.duClient(any())).thenReturn(List.of(enAttente, a03, a04, resilieRecent, resilieAncien, renonce));

        ContexteDeclarationResponse contexte = service.contexte(JETON);

        assertThat(contexte.prenom()).isEqualTo("Camille");
        assertThat(contexte.numeroAssistance()).isEqualTo(FixturesSinistre.ASSISTANCE);
        assertThat(contexte.contrats()).extracting(ContexteDeclarationResponse.ContratDeclarable::numero)
                .containsExactly("AE-RECETTE-A03", "AE-RECETTE-A04", "AE-RECETTE-A09");
    }

    @Test
    void ac1UnAppelDirectSurUnContratNonDeclarableEstRefuse() {
        when(contrats.parNumero("AE-RECETTE-A10")).thenReturn(present(contrat(10L, "AE-RECETTE-A10", "RENONCE",
                "TIERS")));

        assertThatThrownBy(() -> service.creer(new CreationSinistreRequest("AE-RECETTE-A10", "VOL")))
                .isInstanceOf(DeclarationImpossibleException.class);
        assertThatThrownBy(() -> service.types("AE-RECETTE-A10")).isInstanceOf(DeclarationImpossibleException.class);
        verify(sinistres, never()).save(any());
    }

    @Test
    void ac2EtAc7TypesDuProduitAvecLeursPiecesAttendues() {
        TypesSinistreResponse types = service.types("AE-RECETTE-A03");

        assertThat(types.produit()).isEqualTo("AUTO");
        assertThat(types.types()).extracting(TypesSinistreResponse.Type::code)
                .containsExactly("VOL", "BRIS_GLACE", "AUTRE");
        assertThat(types.types().get(0).delaiPrevu()).isEqualTo("2 jours ouvrés");
        assertThat(types.types().get(0).pieces()).extracting(TypesSinistreResponse.PieceAttendue::obligatoire)
                .containsExactly(true, false);
    }

    @Test
    void ac8Rg9ChoisirUnTypeCreeLeBrouillonDansLaCorbeilleDuProduit() {
        DeclarationSinistreResponse declaration = service.creer(new CreationSinistreRequest("AE-RECETTE-A03",
                "VOL"));

        ArgumentCaptor<Sinistre> enregistre = ArgumentCaptor.forClass(Sinistre.class);
        verify(sinistres).save(enregistre.capture());
        assertThat(enregistre.getValue().getStatut()).isEqualTo("BROUILLON");
        assertThat(enregistre.getValue().getCorbeille()).isEqualTo("AUTO");
        assertThat(enregistre.getValue().getRequalifier()).isFalse();
        assertThat(enregistre.getValue().getDeclarationHonneur()).isFalse();
        assertThat(declaration.etape()).isEqualTo(2);
        assertThat(declaration.controles().couvert()).isTrue();
        assertThat(declaration.piecesManquantes()).isTrue();
    }

    @Test
    void ac3UnVolAvecUneFormuleTiersEstSignaleNonCouvertSansBloquer() {
        DeclarationSinistreResponse declaration = service.creer(new CreationSinistreRequest("AE-RECETTE-A04",
                "VOL"));

        assertThat(declaration.controles().couvert()).isFalse();
        assertThat(declaration.statut()).isEqualTo("BROUILLON");
    }

    @Test
    void rg11AutreEstARequalifierSansMessageDeCouverture() {
        DeclarationSinistreResponse declaration = service.creer(new CreationSinistreRequest("AE-RECETTE-A04",
                "AUTRE"));

        assertThat(declaration.controles().couvert()).isNull();
        ArgumentCaptor<Sinistre> enregistre = ArgumentCaptor.forClass(Sinistre.class);
        verify(sinistres).save(enregistre.capture());
        assertThat(enregistre.getValue().getRequalifier()).isTrue();
    }

    @Test
    void ac2UnTypeInconnuDuProduitEstRefuse() {
        assertThatThrownBy(() -> service.creer(new CreationSinistreRequest("AE-RECETTE-A03", "DEGAT_EAUX")))
                .isInstanceOf(DeclarationInvalideException.class);
    }

    @Test
    void rg4LaCouvertureSeLitALaDateDuSinistre() {
        LocalDate dateSinistre = AUJOURDHUI.minusDays(20);
        when(fixtures.options.aLaDate(4L, dateSinistre)).thenReturn(List.of(option("RC"), option("BRIS_GLACE")));
        when(sinistres.findById(42L)).thenReturn(Optional.of(brouillon(42L, 4L, "BRIS_GLACE")));

        DeclarationSinistreResponse declaration = service.mettreAJour(42L, reponses("BRIS_GLACE", dateSinistre,
                false, null, "SOUSCRIPTEUR", null, null));

        assertThat(declaration.controles().couvert()).isTrue();
        verify(fixtures.options).aLaDate(4L, dateSinistre);
    }

    @Test
    void ac5EtAc6DateHorsPeriodeEtDeclarationTardiveSontSignalees() {
        when(sinistres.findById(42L)).thenReturn(Optional.of(brouillon(42L, 3L, "VOL")));
        LocalDate avantLEffet = AUJOURDHUI.minusDays(61);

        DeclarationSinistreResponse declaration = service.mettreAJour(42L, reponses("VOL", avantLEffet, false, null,
                "SOUSCRIPTEUR", null, null));

        assertThat(declaration.controles().horsPeriodeContrat()).isTrue();
        assertThat(declaration.controles().horsDelai()).isTrue();
        assertThat(declaration.controles().delaiPrevu()).isEqualTo("2 jours ouvrés");
        ArgumentCaptor<Sinistre> enregistre = ArgumentCaptor.forClass(Sinistre.class);
        verify(sinistres).save(enregistre.capture());
        assertThat(enregistre.getValue().getHorsPeriodeContrat()).isTrue();
        assertThat(enregistre.getValue().getHorsDelai()).isTrue();
    }

    @Test
    void ac11EtAc13SeulesLesReponsesUtilesSontGardees() {
        when(sinistres.findById(42L)).thenReturn(Optional.of(brouillon(42L, 3L, "VOL")));

        DeclarationSinistreResponse sansBlesses = service.mettreAJour(42L, reponses("VOL", AUJOURDHUI, false, 3,
                "SOUSCRIPTEUR", "Léa Martin", "Fille"));
        assertThat(sansBlesses.nombreBlesses()).isNull();
        assertThat(sansBlesses.declarantNom()).isNull();
        assertThat(sansBlesses.tiers()).isNull();

        DeclarationSinistreResponse proche = service.mettreAJour(42L, reponses("VOL", AUJOURDHUI, true, 2, "PROCHE",
                "Léa Martin", "Fille"));
        assertThat(proche.blesses()).isTrue();
        assertThat(proche.nombreBlesses()).isEqualTo(2);
        assertThat(proche.declarantNom()).isEqualTo("Léa Martin");
        assertThat(proche.declarantLien()).isEqualTo("Fille");
        assertThat(proche.details()).containsEntry("Avez-vous déposé plainte ?", "Oui");
    }

    @Test
    void ac8UneDeclarationEnvoyeeNeSeModifiePlus() {
        Sinistre envoye = brouillon(42L, 3L, "VOL");
        envoye.setStatut("DECLARE");
        when(sinistres.findById(42L)).thenReturn(Optional.of(envoye));

        assertThatThrownBy(() -> service.mettreAJour(42L, reponses("VOL", AUJOURDHUI, false, null, null, null, null)))
                .isInstanceOf(DeclarationDejaEnvoyeeException.class);
    }

    @Test
    void rg6UnBrouillonExpireNestPlusAccessible() {
        Sinistre expire = brouillon(42L, 3L, "VOL");
        expire.setBrouillonExpireLe(MAINTENANT.minusSeconds(1));
        when(sinistres.findById(42L)).thenReturn(Optional.of(expire));

        assertThatThrownBy(() -> service.lire(42L)).isInstanceOf(SinistreInconnuException.class);
        assertThatThrownBy(() -> service.lire(43L)).isInstanceOf(SinistreInconnuException.class);
    }

    @Test
    void ac8MesSinistresListeDossiersEtBrouillons() {
        Sinistre declare = FixturesSinistre.complet(41L, 3L, "VOL");
        declare.setStatut("DECLARE");
        declare.setNumero("SIN-2026-000041");
        declare.setDeclareLe(MAINTENANT);
        Sinistre brouillon = brouillon(42L, 4L, "BRIS_GLACE");
        when(contrats.duClient(any())).thenReturn(List.of(a03, a04));
        when(sinistres.visibles(anyCollection(), eq(MAINTENANT))).thenReturn(List.of(brouillon, declare));

        List<SinistreResumeResponse> liste = service.mesSinistres(JETON);

        assertThat(liste).extracting(SinistreResumeResponse::statut).containsExactly("BROUILLON", "DECLARE");
        assertThat(liste.get(0).libelleType()).isEqualTo("Bris de glace");
        assertThat(liste.get(0).brouillonExpireLe()).isNotNull();
        assertThat(liste.get(1).brouillonExpireLe()).isNull();
        assertThat(liste.get(1).numeroContrat()).isEqualTo("AE-RECETTE-A03");
    }

    @Test
    void mesSinistresSansContratEstVide() {
        when(contrats.duClient(any())).thenReturn(List.of());

        assertThat(service.mesSinistres(JETON)).isEmpty();
        verify(sinistres, never()).visibles(anyCollection(), any());
    }

    static MiseAJourSinistreRequest reponses(String type, LocalDate date, boolean blesses, Integer nombre,
            String declarant, String nom, String lien) {
        return new MiseAJourSinistreRequest(type, 2, date, LocalTime.of(8, 30), "Parking du travail",
                "Constaté le matin", false, new MiseAJourSinistreRequest.Tiers("X", null, null), blesses, nombre,
                declarant, nom, lien, Map.of("Avez-vous déposé plainte ?", "Oui"), null, null);
    }
}
