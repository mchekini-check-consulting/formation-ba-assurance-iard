package net.checkconsulting.assuranceiard.api.securite;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * US-04 : route ouverte sans contrôle de droits (devis sans compte, inscription, connexion, lecture de la session).
 * À déclarer explicitement : sans cette annotation ni {@link Acces}, la route est refusée (RG2).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccesPublic {
}
