package net.checkconsulting.assuranceiard.api.repository;

import java.sql.Timestamp;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * US-24, RG6 : supprime les brouillons de sinistre expirés, avec leurs pièces jointes (le contenu des pièces part
 * avec elles, ON DELETE CASCADE). Un dossier déclaré n'est jamais supprimé.
 */
@Repository
@RequiredArgsConstructor
public class PurgeBrouillonsRepository {

    private static final String EXPIRES = "SELECT id FROM sinistre WHERE statut = 'BROUILLON' AND brouillon_expire_le <= ?";

    private final JdbcTemplate jdbc;

    /** @return nombre de brouillons supprimés */
    @LogMetier
    public int supprimerExpires(Instant maintenant) {
        Timestamp limite = Timestamp.from(maintenant);
        jdbc.update("DELETE FROM piece_jointe WHERE sinistre_id IN (" + EXPIRES + ")", limite);
        return jdbc.update("DELETE FROM sinistre WHERE id IN (" + EXPIRES + ")", limite);
    }
}
