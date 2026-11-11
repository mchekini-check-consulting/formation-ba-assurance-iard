package net.checkconsulting.assuranceiard.api.securite;

import java.util.Optional;
import java.util.UUID;

/** US-04 : lecture d'un identifiant de route ; une valeur absente ou mal formée ne désigne aucune ressource. */
final class Identifiants {

    private Identifiants() {
    }

    static Optional<UUID> uuid(String valeur) {
        if (valeur == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(valeur));
        } catch (IllegalArgumentException mauvaisFormat) {
            return Optional.empty();
        }
    }

    static Optional<Long> nombre(String valeur) {
        if (valeur == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(valeur));
        } catch (NumberFormatException mauvaisFormat) {
            return Optional.empty();
        }
    }
}
