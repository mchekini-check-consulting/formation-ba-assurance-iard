package net.checkconsulting.assuranceiard.api.securite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.checkconsulting.assuranceiard.api.AssuranceIardApplication;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisEnregistreRepository;
import net.checkconsulting.assuranceiard.api.repository.DossierSouscriptionRepository;
import net.checkconsulting.assuranceiard.api.repository.JustificatifRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-04, RG2 : chaque route de l'API déclare sa règle d'accès (@Acces) ou se dit publique (@AccesPublic), et
 * l'identifiant déclaré existe bien parmi ses paramètres. Une route oubliée serait de toute façon refusée.
 */
class RoutesDeclareesTest {

    private static List<Method> routes() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scan = new ClassPathScanningCandidateComponentProvider(false);
        scan.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Method> routes = new ArrayList<>();
        for (var definition : scan.findCandidateComponents(AssuranceIardApplication.class.getPackageName())) {
            Class<?> classe = ClassUtils.forName(definition.getBeanClassName(), null);
            Arrays.stream(classe.getDeclaredMethods())
                    .filter(m -> AnnotatedElementUtils.hasAnnotation(m, RequestMapping.class))
                    .forEach(routes::add);
        }
        return routes;
    }

    @Test
    void chaqueRouteDeclareSaRegleDAcces() throws ClassNotFoundException {
        List<Method> routes = routes();
        assertThat(routes).hasSizeGreaterThan(20);

        List<String> sansRegle = routes.stream()
                .filter(m -> !ControleAccesAspect.estPublique(m) && !m.isAnnotationPresent(Acces.class))
                .map(m -> m.getDeclaringClass().getSimpleName() + "." + m.getName())
                .toList();

        assertThat(sansRegle).as("routes sans @Acces ni @AccesPublic").isEmpty();
    }

    @Test
    void lIdentifiantDeclareEstUnParametreDeLaRoute() throws ClassNotFoundException {
        List<String> erreurs = new ArrayList<>();
        for (Method route : routes()) {
            Acces acces = route.getAnnotation(Acces.class);
            if (acces == null) {
                continue;
            }
            boolean ressource = acces.ressource() != TypeRessource.AUCUNE;
            String parametre = acces.identifiant().split("\\.")[0];
            boolean trouve = Arrays.stream(route.getParameters()).map(Parameter::getName).anyMatch(parametre::equals);
            if (ressource != trouve) {
                erreurs.add(route.getDeclaringClass().getSimpleName() + "." + route.getName());
            }
        }
        assertThat(erreurs).as("routes dont l'identifiant de ressource est absent ou superflu").isEmpty();
    }

    @Test
    void chaqueRessourceAUnControleDeProprietaire() {
        List<TypeRessource> attendues = Arrays.stream(TypeRessource.values())
                .filter(type -> type != TypeRessource.AUCUNE)
                .toList();
        List<TypeRessource> controlees = List.of(
                new ProprietairesDevis(mock(DevisEnregistreRepository.class)).type(),
                new ProprietairesSouscription(mock(DossierSouscriptionRepository.class)).type(),
                new ProprietairesContrat(mock(ContratRepository.class)).type(),
                new ProprietairesDocument(mock(JustificatifRepository.class)).type());

        assertThat(controlees).containsExactlyInAnyOrderElementsOf(attendues);
    }
}
