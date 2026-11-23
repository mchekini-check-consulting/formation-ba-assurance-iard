package net.checkconsulting.assuranceiard.api.securite;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.exception.AccesNonAutoriseException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * US-04, RG1 : contrôle centralisé, avant chaque route de l'API (aucune vérification copiée dans les contrôleurs).
 * Une route sans {@link Acces} ni {@link AccesPublic} est refusée (RG2).
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ControleAccesAspect {

    private final SessionService sessionService;
    private final ControleAccesService controle;

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object controler(ProceedingJoinPoint point) throws Throwable {
        Method methode = AopUtils.getMostSpecificMethod(
                ((MethodSignature) point.getSignature()).getMethod(), AopUtils.getTargetClass(point.getTarget()));
        if (estPublique(methode)) {
            return point.proceed();
        }
        Acces acces = methode.getAnnotation(Acces.class);
        if (acces == null) {
            log.error("Route refusée faute de règle d'accès déclarée : {}.{}",
                    methode.getDeclaringClass().getSimpleName(), methode.getName());
            throw new AccesNonAutoriseException();
        }
        HttpServletRequest requete = requeteCourante();
        Compte compte = sessionService.compteConnecte(requete.getHeader(HttpHeaders.AUTHORIZATION));
        String identifiant = identifiant(acces.identifiant(), methode, point.getArgs());
        controle.verifier(compte, acces, identifiant,
                new ControleAccesService.Requete(requete.getMethod(), requete.getRequestURI()));
        return point.proceed();
    }

    static boolean estPublique(Method methode) {
        return methode.isAnnotationPresent(AccesPublic.class)
                || methode.getDeclaringClass().isAnnotationPresent(AccesPublic.class);
    }

    private static HttpServletRequest requeteCourante() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }

    /** « reference » : le paramètre de ce nom ; « demande.referenceDevis » : le champ du corps de la requête. */
    static String identifiant(String chemin, Method methode, Object... arguments) {
        if (chemin.isEmpty()) {
            return null;
        }
        String[] morceaux = chemin.split("\\.", 2);
        Parameter[] parametres = methode.getParameters();
        for (int i = 0; i < parametres.length; i++) {
            if (parametres[i].getName().equals(morceaux[0])) {
                Object valeur = morceaux.length == 1 ? arguments[i] : champ(arguments[i], morceaux[1]);
                return valeur == null ? null : valeur.toString();
            }
        }
        throw new IllegalStateException("Identifiant introuvable dans la route : " + chemin);
    }

    private static Object champ(Object objet, String nom) {
        if (objet == null) {
            return null;
        }
        RecordComponent[] composants = objet.getClass().getRecordComponents();
        if (composants != null) {
            for (RecordComponent composant : composants) {
                if (composant.getName().equals(nom)) {
                    return lire(composant, objet);
                }
            }
        }
        throw new IllegalStateException("Champ introuvable : " + nom);
    }

    private static Object lire(RecordComponent composant, Object objet) {
        try {
            return composant.getAccessor().invoke(objet);
        } catch (ReflectiveOperationException erreur) {
            throw new IllegalStateException("Champ illisible : " + composant.getName(), erreur);
        }
    }
}
