package net.checkconsulting.assuranceiard.api.service.avenant;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.checkconsulting.assuranceiard.api.model.Avenant;

/**
 * US-23 : numéro d'avenant AV-AAAA-NNNNNN, sur le modèle des factures (FAC-AAAA-NNNNNN) : année de préparation
 * (heure de Paris) et identifiant de l'avenant, qui ne revient jamais en arrière. Rien n'est stocké en plus : le
 * numéro se recalcule, et il sert de référence au document PDF (document.reference).
 */
public final class NumeroAvenant {

    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");
    private static final Pattern FORMAT = Pattern.compile("^AV-\\d{4}-(\\d{6,})$");

    private NumeroAvenant() {
    }

    public static String de(Avenant avenant) {
        Instant creation = avenant.getDateCreation() == null ? Instant.now() : avenant.getDateCreation();
        return de(creation.atZone(FRANCE).getYear(), avenant.getId());
    }

    static String de(int annee, long identifiant) {
        return "AV-%d-%06d".formatted(annee, identifiant);
    }

    /** Identifiant de l'avenant d'un numéro AV-AAAA-NNNNNN ; vide si ce n'en est pas un. */
    public static Optional<Long> identifiant(String numero) {
        if (numero == null) {
            return Optional.empty();
        }
        Matcher correspondance = FORMAT.matcher(numero);
        return correspondance.matches() ? Optional.of(Long.parseLong(correspondance.group(1))) : Optional.empty();
    }
}
