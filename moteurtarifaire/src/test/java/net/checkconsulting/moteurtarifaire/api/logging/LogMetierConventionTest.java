package net.checkconsulting.moteurtarifaire.api.logging;

import static org.assertj.core.api.Assertions.assertThat;

import net.checkconsulting.moteurtarifaire.api.MoteurTarifaireApplication;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.util.ClassUtils;

class LogMetierConventionTest {

    private static final String PAQUET = MoteurTarifaireApplication.class.getPackageName();

    private static List<Class<?>> classesDeLApplication() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scan = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
                return definition.getMetadata().isIndependent();
            }
        };
        scan.addIncludeFilter((lecteur, fabrique) -> true);
        List<Class<?>> classes = new ArrayList<>();
        for (var definition : scan.findCandidateComponents(PAQUET)) {
            Class<?> classe = ClassUtils.forName(definition.getBeanClassName(), null);
            if (!classe.getProtectionDomain().getCodeSource().getLocation().getPath().contains("test-classes")) {
                classes.add(classe);
            }
        }
        return classes;
    }

    @Test
    void lesNomsDesParametresSontCompiles() throws NoSuchMethodException {
        Method methode = Champs.class.getMethod("erreurs", Method.class);
        assertThat(methode.getParameters()[0].getName()).isEqualTo("methode");
    }

    @Test
    void lesChampsApprouvesSontConformes() throws ClassNotFoundException {
        List<Class<?>> classes = classesDeLApplication();
        assertThat(classes).contains(MoteurTarifaireApplication.class, LogMetier.class);

        List<String> erreurs = classes.stream()
                .flatMap(classe -> Arrays.stream(classe.getDeclaredMethods()))
                .flatMap(methode -> Champs.erreurs(methode).stream())
                .toList();

        assertThat(erreurs).as("champs de @LogMetier à corriger").isEmpty();
    }
}
