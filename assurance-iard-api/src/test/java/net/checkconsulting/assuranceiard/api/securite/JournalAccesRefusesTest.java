package net.checkconsulting.assuranceiard.api.securite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import net.checkconsulting.assuranceiard.api.dto.AccesRefuseResponse;
import net.checkconsulting.assuranceiard.api.model.AccesRefuse;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Profil;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.repository.AccesRefuseRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JournalAccesRefusesTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-11T10:00:00Z");

    private final AccesRefuseRepository repository = mock(AccesRefuseRepository.class);
    private final JournalAccesRefuses journal =
            new JournalAccesRefuses(repository, Clock.fixed(MAINTENANT, ZoneOffset.UTC));

    @Acces(rubrique = Rubrique.DOCUMENTS, droit = Droit.CONSULTER, ressource = TypeRessource.DOCUMENT,
            identifiant = "id")
    void document() {
        // Route-modèle : seule son annotation @Acces est lue.
    }

    @Test
    void rg3EnregistreDateUtilisateurRessourceEtAction() throws NoSuchMethodException {
        Method route = JournalAccesRefusesTest.class.getDeclaredMethod("document");
        Compte compte = new Compte();
        compte.setId(7L);
        compte.setEmail("alice@exemple.fr");
        compte.setProfil(Profil.ASSURE);

        journal.enregistrer(compte, route.getAnnotation(Acces.class), "42",
                new ControleAccesService.Requete("GET", "/documents/42"), MotifRefus.PAS_PROPRIETAIRE);

        ArgumentCaptor<AccesRefuse> ligne = ArgumentCaptor.forClass(AccesRefuse.class);
        verify(repository).save(ligne.capture());
        assertThat(ligne.getValue()).satisfies(l -> {
            assertThat(l.getDateTentative()).isEqualTo(MAINTENANT);
            assertThat(l.getCompteId()).isEqualTo(7L);
            assertThat(l.getEmail()).isEqualTo("alice@exemple.fr");
            assertThat(l.getProfil()).isEqualTo(Profil.ASSURE);
            assertThat(l.getMethode()).isEqualTo("GET");
            assertThat(l.getChemin()).isEqualTo("/documents/42");
            assertThat(l.getRessource()).isEqualTo(TypeRessource.DOCUMENT);
            assertThat(l.getIdentifiant()).isEqualTo("42");
            assertThat(l.getRubrique()).isEqualTo(Rubrique.DOCUMENTS);
            assertThat(l.getDroit()).isEqualTo(Droit.CONSULTER);
            assertThat(l.getMotif()).isEqualTo(MotifRefus.PAS_PROPRIETAIRE);
        });
    }

    @Test
    void renvoieLesDernieresTentatives() {
        AccesRefuse ligne = new AccesRefuse();
        ligne.setDateTentative(MAINTENANT);
        ligne.setEmail("alice@exemple.fr");
        ligne.setProfil(Profil.ASSURE);
        ligne.setMethode("GET");
        ligne.setChemin("/back-office/clients");
        ligne.setRessource(TypeRessource.AUCUNE);
        ligne.setRubrique(Rubrique.COMPTES_UTILISATEURS);
        ligne.setDroit(Droit.CONSULTER);
        ligne.setMotif(MotifRefus.BACK_OFFICE);
        when(repository.findTop200ByOrderByDateTentativeDesc()).thenReturn(List.of(ligne));

        assertThat(journal.derniers()).containsExactly(new AccesRefuseResponse(MAINTENANT, "alice@exemple.fr",
                "ASSURE", "GET", "/back-office/clients", "AUCUNE", null, "COMPTES_UTILISATEURS", "CONSULTER",
                "BACK_OFFICE"));
    }

    @Test
    void tronqueLesTextesTropLongs() {
        assertThat(JournalAccesRefuses.tronquer(null, 3)).isNull();
        assertThat(JournalAccesRefuses.tronquer("abc", 3)).isEqualTo("abc");
        assertThat(JournalAccesRefuses.tronquer("abcdef", 3)).isEqualTo("abc");
    }
}
