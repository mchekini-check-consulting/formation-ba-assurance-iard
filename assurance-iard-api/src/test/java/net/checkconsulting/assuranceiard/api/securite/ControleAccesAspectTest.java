package net.checkconsulting.assuranceiard.api.securite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.exception.AccesNonAutoriseException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ControleAccesAspectTest {

    private final SessionService sessionService = mock(SessionService.class);
    private final ControleAccesService controle = mock(ControleAccesService.class);
    private final ControleAccesAspect aspect = new ControleAccesAspect(sessionService, controle);

    /** Corps de requête-modèle. */
    record Demande(UUID referenceDevis) {
    }

    /** Contrôleur-modèle : une route publique, une route protégée, une route oubliée. */
    static class Routes {

        @AccesPublic
        public String publique() {
            return "ouvert";
        }

        @Acces(rubrique = Rubrique.CONTRATS, droit = Droit.CONSULTER, ressource = TypeRessource.CONTRAT,
                identifiant = "demande.referenceDevis")
        public String protegee(Demande demande) {
            return "contrat";
        }

        public String oubliee() {
            return "jamais";
        }
    }

    @BeforeEach
    void preparerRequete() {
        MockHttpServletRequest requete = new MockHttpServletRequest("POST", "/devis/contrat");
        requete.addHeader(HttpHeaders.AUTHORIZATION, "Bearer jeton");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requete));
    }

    @AfterEach
    void oublierRequete() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static ProceedingJoinPoint appel(String nom, Object... arguments) throws Throwable {
        Method methode = java.util.Arrays.stream(Routes.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(nom)).findFirst().orElseThrow();
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(methode);
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.getSignature()).thenReturn(signature);
        when(point.getTarget()).thenReturn(new Routes());
        when(point.getArgs()).thenReturn(arguments);
        when(point.proceed()).thenReturn("fait");
        return point;
    }

    @Test
    void uneRoutePubliquePasseSansSession() throws Throwable {
        assertThat(aspect.controler(appel("publique"))).isEqualTo("fait");
        verifyNoInteractions(sessionService, controle);
    }

    @Test
    void rg2UneRouteSansRegleDeclareeEstRefusee() throws Throwable {
        ProceedingJoinPoint point = appel("oubliee");
        assertThatThrownBy(() -> aspect.controler(point)).isInstanceOf(AccesNonAutoriseException.class);
        verifyNoInteractions(sessionService, controle);
    }

    @Test
    void uneRouteProtegeeEstControleeAvecLIdentifiantDuCorps() throws Throwable {
        UUID reference = UUID.randomUUID();
        Compte compte = new Compte();
        when(sessionService.compteConnecte("Bearer jeton")).thenReturn(compte);

        assertThat(aspect.controler(appel("protegee", new Demande(reference)))).isEqualTo("fait");
        verify(controle).verifier(eq(compte), any(Acces.class), eq(reference.toString()),
                eq(new ControleAccesService.Requete("POST", "/devis/contrat")));
    }

    @Test
    void lIdentifiantSeLitDansUnParametreOuUnCorps() throws NoSuchMethodException {
        Method protegee = Routes.class.getDeclaredMethod("protegee", Demande.class);
        UUID reference = UUID.randomUUID();

        assertThat(ControleAccesAspect.identifiant("", protegee, new Demande(reference))).isNull();
        assertThat(ControleAccesAspect.identifiant("demande", protegee, "valeur")).isEqualTo("valeur");
        assertThat(ControleAccesAspect.identifiant("demande.referenceDevis", protegee, new Demande(reference)))
                .isEqualTo(reference.toString());
        assertThat(ControleAccesAspect.identifiant("demande.referenceDevis", protegee, (Object) null)).isNull();
        assertThat(ControleAccesAspect.identifiant("demande.referenceDevis", protegee, new Demande(null))).isNull();
    }

    @Test
    void unIdentifiantMalDeclareEstUneErreurDeConfiguration() throws NoSuchMethodException {
        Method protegee = Routes.class.getDeclaredMethod("protegee", Demande.class);
        Demande demande = new Demande(UUID.randomUUID());

        assertThatThrownBy(() -> ControleAccesAspect.identifiant("inconnu", protegee, demande))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ControleAccesAspect.identifiant("demande.inconnu", protegee, demande))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ControleAccesAspect.identifiant("demande.x", protegee, "pas un record"))
                .isInstanceOf(IllegalStateException.class);
    }
}
