package net.checkconsulting.assuranceiard.api.securite;

import java.util.Optional;
import java.util.UUID;

/** US-04 : lecture d'un identifiant de route ; une valeur mal formée ne désigne aucune ressource. */
final class Identifiants {

    private Identifiants() {
    }

    static Optional<UUID> uuid(String valeur) {
        try {
            return Optional.of(UUID.fromString(valeur));
        } catch (IllegalArgumentException | NullPointerException mauvaisFormat) {
            return Optional.empty();
        }
    }

    static Optional<Long> nombre(String valeur) {
        try {
            return Optional.of(Long.valueOf(valeur));
        } catch (NumberFormatException mauvaisFormat) {
            return Optional.empty();
        }
    }
}
