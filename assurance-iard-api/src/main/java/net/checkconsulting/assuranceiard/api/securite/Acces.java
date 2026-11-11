package net.checkconsulting.assuranceiard.api.securite;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Rubrique;

/**
 * US-04 : ce qu'une route demande. Contrôlé par {@link ControleAccesAspect} avant chaque appel : session valable,
 * droit accordé au profil (tableau de l'US-02, relu à chaque requête), propriétaire de la ressource pour un client,
 * profil interne pour le back-office. Une route sans {@code @Acces} ni {@link AccesPublic} est refusée (RG2).
 *
 * <p>Exemple : {@code @Acces(rubrique = Rubrique.CONTRATS, droit = Droit.CONSULTER,
 * ressource = TypeRessource.CONTRAT, identifiant = "referenceDevis")}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Acces {

    Rubrique rubrique();

    Droit droit();

    /** Ressource visée, dont le propriétaire est vérifié pour un client (Prospect, Assuré). */
    TypeRessource ressource() default TypeRessource.AUCUNE;

    /** Paramètre de la route qui porte l'identifiant : « reference », ou « demande.referenceDevis » dans le corps. */
    String identifiant() default "";

    /** Route du back-office : réservée aux profils internes (Gestionnaire, Administrateur), quels que soient les droits. */
    boolean backOffice() default false;
}
