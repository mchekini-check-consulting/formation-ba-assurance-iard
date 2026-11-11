package net.checkconsulting.assuranceiard.api.securite;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import net.checkconsulting.assuranceiard.api.exception.AccesNonAutoriseException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Profil;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.repository.PermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ControleAccesServiceTest {

    private static final ControleAccesService.Requete REQUETE =
            new ControleAccesService.Requete("GET", "/devis/contrat/ref");
    private static final String REFERENCE = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

    @Mock
    private PermissionRepository permissions;

    @Mock
    private JournalAccesRefuses journal;

    @Mock
    private RessourceProtegee contrats;

    private ControleAccesService controle;

    @BeforeEach
    void preparer() {
        when(contrats.type()).thenReturn(TypeRessource.CONTRAT);
        controle = new ControleAccesService(permissions, journal, List.of(contrats));
    }

    // Annotations-modèles, lues comme sur une vraie route
    @Acces(rubrique = Rubrique.CONTRATS, droit = Droit.CONSULTER, ressource = TypeRessource.CONTRAT,
            identifiant = "reference")
    void contrat() {
    }

    @Acces(rubrique = Rubrique.PROFILS, droit = Droit.CONSULTER, backOffice = true)
    void backOffice() {
    }

    @Acces(rubrique = Rubrique.SINISTRES, droit = Droit.CONSULTER, ressource = TypeRessource.DOCUMENT,
            identifiant = "id")
    void ressourceSansControle() {
    }

    private static Acces acces(String methode) throws NoSuchMethodException {
        Method m = ControleAccesServiceTest.class.getDeclaredMethod(methode);
        return m.getAnnotation(Acces.class);
    }

    private static Compte compte(long id, Profil profil) {
        Compte compte = new Compte();
        compte.setId(id);
        compte.setEmail("compte" + id + "@exemple.fr");
        compte.setProfil(profil);
        return compte;
    }

    private void droitAccorde(Profil profil, Rubrique rubrique, Droit droit, boolean valeur) {
        when(permissions.existsByProfilAndRubriqueAndDroitAndValeurTrue(profil, rubrique, droit)).thenReturn(valeur);
    }

    @Test
    void ac1LAssureOuvreSonPropreContrat() throws Exception {
        droitAccorde(Profil.ASSURE, Rubrique.CONTRATS, Droit.CONSULTER, true);
        when(contrats.proprietaires(REFERENCE)).thenReturn(Set.of(1L));

        assertThatCode(() -> controle.verifier(compte(1, Profil.ASSURE), acces("contrat"), REFERENCE, REQUETE))
                .doesNotThrowAnyException();
        verifyNoInteractions(journal);
    }

    @Test
    void ac2LeContratDUnAutreEstRefuseEtJournalise() throws Exception {
        Compte assureA = compte(1, Profil.ASSURE);
        Acces acces = acces("contrat");
        droitAccorde(Profil.ASSURE, Rubrique.CONTRATS, Droit.CONSULTER, true);
        when(contrats.proprietaires(REFERENCE)).thenReturn(Set.of(2L));

        assertThatThrownBy(() -> controle.verifier(assureA, acces, REFERENCE, REQUETE))
                .isInstanceOf(AccesNonAutoriseException.class)
                .hasMessage("Accès non autorisé.");
        verify(journal).enregistrer(assureA, acces, REFERENCE, REQUETE, MotifRefus.PAS_PROPRIETAIRE);
    }

    @Test
    void uneRessourceIntrouvableNEstPasRefuseeIci() throws Exception {
        droitAccorde(Profil.ASSURE, Rubrique.CONTRATS, Droit.CONSULTER, true);
        when(contrats.proprietaires(REFERENCE)).thenReturn(Set.of());

        assertThatCode(() -> controle.verifier(compte(1, Profil.ASSURE), acces("contrat"), REFERENCE, REQUETE))
                .doesNotThrowAnyException();
    }

    @Test
    void rg2UnDroitNonAccordeEstRefuse() throws Exception {
        Compte assure = compte(1, Profil.ASSURE);
        Acces acces = acces("contrat");
        droitAccorde(Profil.ASSURE, Rubrique.CONTRATS, Droit.CONSULTER, false);

        assertThatThrownBy(() -> controle.verifier(assure, acces, REFERENCE, REQUETE))
                .isInstanceOf(AccesNonAutoriseException.class);
        verify(journal).enregistrer(assure, acces, REFERENCE, REQUETE, MotifRefus.DROIT_ABSENT);
        verify(contrats, never()).proprietaires(any());
    }

    @Test
    void ac3UnAssureNeVoitJamaisLeBackOffice() throws Exception {
        Compte assure = compte(1, Profil.ASSURE);
        Acces acces = acces("backOffice");

        assertThatThrownBy(() -> controle.verifier(assure, acces, null, REQUETE))
                .isInstanceOf(AccesNonAutoriseException.class);
        verify(journal).enregistrer(assure, acces, null, REQUETE, MotifRefus.BACK_OFFICE);
        verifyNoInteractions(permissions);
    }

    @Test
    void leGestionnaireConsulteLesContratsDeTousLesClients() throws Exception {
        droitAccorde(Profil.GESTIONNAIRE, Rubrique.CONTRATS, Droit.CONSULTER, true);

        assertThatCode(() -> controle.verifier(compte(9, Profil.GESTIONNAIRE), acces("contrat"), REFERENCE, REQUETE))
                .doesNotThrowAnyException();
        verify(contrats, never()).proprietaires(any());
    }

    @Test
    void lAdministrateurEntreDansLeBackOfficeAvecLeDroit() throws Exception {
        droitAccorde(Profil.ADMINISTRATEUR, Rubrique.PROFILS, Droit.CONSULTER, true);

        assertThatCode(() -> controle.verifier(compte(9, Profil.ADMINISTRATEUR), acces("backOffice"), null, REQUETE))
                .doesNotThrowAnyException();
    }

    @Test
    void uneRessourceSansControleDeProprietaireEstUneErreurDeConfiguration() throws Exception {
        Compte assure = compte(1, Profil.ASSURE);
        Acces acces = acces("ressourceSansControle");
        droitAccorde(Profil.ASSURE, Rubrique.SINISTRES, Droit.CONSULTER, true);

        assertThatThrownBy(() -> controle.verifier(assure, acces, "1", REQUETE))
                .isInstanceOf(IllegalStateException.class);
    }
}
