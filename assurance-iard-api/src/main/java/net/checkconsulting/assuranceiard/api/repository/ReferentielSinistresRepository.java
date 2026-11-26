package net.checkconsulting.assuranceiard.api.repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * US-24, RG1 et RG10 : types de sinistres et pièces attendues par produit (tables ref_type_sinistre et
 * ref_piece_attendue, V36). « Autre / je ne sais pas » est toujours renvoyé en dernier, quel que soit son ordre.
 */
@Repository
@RequiredArgsConstructor
public class ReferentielSinistresRepository {

    public static final String AUTRE = "AUTRE";

    private static final String TYPES = """
            SELECT produit, code, libelle, aide, delai_jours_ouvres, delai_jours_arrete, formules_couvrantes,
                   options_couvrantes, gestes_urgence
            FROM ref_type_sinistre
            WHERE produit = ? AND (actif OR code = 'AUTRE')
            ORDER BY (code = 'AUTRE'), ordre, code""";
    private static final String PIECES = """
            SELECT code, libelle, obligatoire FROM ref_piece_attendue
            WHERE produit = ? AND type_sinistre = ? ORDER BY ordre, id""";

    private final JdbcTemplate jdbc;

    /** Un type de sinistre proposé pour un produit. Délais : RG2 (vide si non applicable). */
    public record TypeSinistre(String produit, String code, String libelle, String aide, Integer delaiJoursOuvres,
            Integer delaiJoursArrete, List<String> formulesCouvrantes, List<String> optionsCouvrantes,
            List<String> gestesUrgence) {

        public boolean autre() {
            return AUTRE.equals(code);
        }
    }

    /** Une pièce attendue pour un type de sinistre. */
    public record PieceAttendue(String code, String libelle, boolean obligatoire) {
    }

    /** AC2 : types actifs du produit, dans l'ordre d'affichage, « Autre » en dernier. */
    @LogMetier
    public List<TypeSinistre> types(String produit) {
        return jdbc.query(TYPES, (ligne, rang) -> type(ligne), produit);
    }

    @LogMetier
    public Optional<TypeSinistre> type(String produit, String code) {
        return types(produit).stream().filter(type -> type.code().equals(code)).findFirst();
    }

    /** AC7 : pièces attendues pour ce type, obligatoires et facultatives. */
    @LogMetier
    public List<PieceAttendue> pieces(String produit, String type) {
        return jdbc.query(PIECES, (ligne, rang) -> new PieceAttendue(ligne.getString("code"),
                ligne.getString("libelle"), ligne.getBoolean("obligatoire")), produit, type);
    }

    private static TypeSinistre type(ResultSet ligne) throws SQLException {
        return new TypeSinistre(ligne.getString("produit"), ligne.getString("code"), ligne.getString("libelle"),
                ligne.getString("aide"), ligne.getObject("delai_jours_ouvres", Integer.class),
                ligne.getObject("delai_jours_arrete", Integer.class), textes(ligne.getArray("formules_couvrantes")),
                textes(ligne.getArray("options_couvrantes")), textes(ligne.getArray("gestes_urgence")));
    }

    private static List<String> textes(Array tableau) throws SQLException {
        return tableau == null ? List.of() : Arrays.asList((String[]) tableau.getArray());
    }
}
