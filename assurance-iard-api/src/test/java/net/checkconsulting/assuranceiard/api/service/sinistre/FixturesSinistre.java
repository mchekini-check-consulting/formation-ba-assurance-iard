package net.checkconsulting.assuranceiard.api.service.sinistre;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.config.SinistresProperties;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.ContratOption;
import net.checkconsulting.assuranceiard.api.model.Sinistre;
import net.checkconsulting.assuranceiard.api.repository.ContratOptionRepository;
import net.checkconsulting.assuranceiard.api.repository.PieceJointeRepository;
import net.checkconsulting.assuranceiard.api.repository.ReferentielSinistresRepository;
import net.checkconsulting.assuranceiard.api.repository.ReferentielSinistresRepository.PieceAttendue;
import net.checkconsulting.assuranceiard.api.repository.ReferentielSinistresRepository.TypeSinistre;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * US-24 : jeu de test commun aux tests des services de déclaration, sur le modèle des données de recette de Camille
 * (référentiel de V37 : mêmes types, délais, garanties et pièces).
 */
final class FixturesSinistre {

    static final Instant MAINTENANT = Instant.parse("2026-11-26T10:00:00Z");
    static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 26);
    static final Clock HORLOGE = Clock.fixed(MAINTENANT, ZoneOffset.UTC);
    static final String ASSISTANCE = "01 23 45 67 89";

    static final TypeSinistre VOL = new TypeSinistre("AUTO", "VOL", "Vol du véhicule", null, 2, null,
            List.of("INTER", "TR"), List.of(), List.of());
    static final TypeSinistre BRIS_GLACE = new TypeSinistre("AUTO", "BRIS_GLACE", "Bris de glace", null, 5, null,
            List.of("INTER", "TR"), List.of("BRIS_GLACE"), List.of());
    static final TypeSinistre AUTRE = new TypeSinistre("AUTO", "AUTRE", "Autre / je ne sais pas", null, 5, null,
            List.of(), List.of(), List.of());
    static final List<TypeSinistre> TYPES_AUTO = List.of(VOL, BRIS_GLACE, AUTRE);
    static final List<PieceAttendue> PIECES_VOL = List.of(
            new PieceAttendue("RECEPISSE_PLAINTE", "Récépissé de dépôt de plainte", true),
            new PieceAttendue("FACTURES_EQUIPEMENTS", "Factures d'équipements", false));

    final ReferentielSinistresRepository referentiel = mock(ReferentielSinistresRepository.class);
    final PieceJointeRepository pieces = mock(PieceJointeRepository.class);
    final ContratOptionRepository options = mock(ContratOptionRepository.class);
    final JsonSinistre json = new JsonSinistre(JsonMapper.builder().build());
    final SinistresProperties proprietes = new SinistresProperties(ASSISTANCE);
    final VueDeclaration vue = new VueDeclaration(referentiel, pieces, new CouvertureContrat(options), json,
            proprietes, HORLOGE);

    FixturesSinistre() {
        when(referentiel.types("AUTO")).thenReturn(TYPES_AUTO);
        when(referentiel.type(eq("AUTO"), anyString())).thenAnswer(appel -> TYPES_AUTO.stream()
                .filter(type -> type.code().equals(appel.getArgument(1))).findFirst());
        when(referentiel.pieces(eq("AUTO"), anyString())).thenAnswer(appel ->
                "VOL".equals(appel.getArgument(1)) ? PIECES_VOL : List.of());
        when(pieces.findBySinistreIdAndStatutOrderByIdAsc(any(), eq("STOCKEE"))).thenReturn(List.of());
        when(options.aLaDate(anyLong(), any())).thenReturn(List.of(option("RC")));
    }

    static Compte camille() {
        Compte compte = new Compte();
        ReflectionTestUtils.setField(compte, "id", 2L);
        compte.setPrenom("Camille");
        compte.setEmail("recette.auto@assureasy.fr");
        return compte;
    }

    /** Contrat auto de Camille (formule, statut, effet il y a 60 jours). */
    static Contrat contrat(long id, String numero, String statut, String formule) {
        Contrat contrat = new Contrat();
        contrat.setId(id);
        contrat.setNumero(numero);
        contrat.setStatut(statut);
        contrat.setDateEffet(AUJOURDHUI.minusDays(60));
        contrat.setProduit("AUTO");
        contrat.setFormule(formule);
        contrat.setCompte(camille());
        return contrat;
    }

    static ContratOption option(String code) {
        ContratOption option = new ContratOption();
        option.setCodeOption(code);
        return option;
    }

    static Sinistre brouillon(long id, long contratId, String type) {
        Sinistre sinistre = new Sinistre();
        sinistre.setId(id);
        sinistre.setContratId(contratId);
        sinistre.setType(type);
        sinistre.setCorbeille("AUTO");
        sinistre.setEtape((short) 2);
        sinistre.setBrouillonExpireLe(MAINTENANT.plusSeconds(86_400));
        sinistre.setDateCreation(MAINTENANT.minusSeconds(3_600));
        return sinistre;
    }

    static Sinistre complet(long id, long contratId, String type) {
        Sinistre sinistre = brouillon(id, contratId, type);
        sinistre.setDateSurvenance(AUJOURDHUI.minusDays(1));
        sinistre.setLieu("Parking du travail");
        sinistre.setDescription("Véhicule disparu le matin");
        sinistre.setTiersImplique(false);
        sinistre.setBlesses(false);
        sinistre.setDeclarant("SOUSCRIPTEUR");
        sinistre.setEtape((short) 3);
        return sinistre;
    }

    static Optional<Contrat> present(Contrat contrat) {
        return Optional.of(contrat);
    }
}
