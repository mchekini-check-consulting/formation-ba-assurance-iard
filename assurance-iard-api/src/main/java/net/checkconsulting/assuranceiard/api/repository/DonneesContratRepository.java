package net.checkconsulting.assuranceiard.api.repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * T-03 : lecture d'un contrat pour ses documents (contrat, client, logement ou véhicule, conducteur, mandat, options).
 * En SQL direct : l'entité Contrat du sprint 1 ne connaît pas encore les colonnes T-05 (produit, véhicule...).
 */
@Repository
@RequiredArgsConstructor
public class DonneesContratRepository {

    private static final String CONTRAT = """
            SELECT c.id, c.numero, c.produit, c.formule,
                   co.civilite, co.prenom, co.nom,
                   c.type_logement, c.surface, c.adresse_logement, c.code_postal, c.ville,
                   v.immatriculation, v.marque, v.modele, v.version,
                   cd.coefficient_bonus_malus, cd.est_souscripteur,
                   c.franchise, c.cotisation_mensuelle, c.cotisation_annuelle, c.date_effet, c.date_fin,
                   c.frequence_paiement, c.jour_prelevement, m.iban_masque, m.rum,
                   rh.ancien_assureur, rh.ancien_numero
            FROM contrat c
            JOIN compte co ON co.id = c.compte_id
            LEFT JOIN mandat_sepa m ON m.id = c.mandat_sepa_id
            LEFT JOIN vehicule v ON v.id = c.vehicule_id
            LEFT JOIN conducteur cd ON cd.id = c.conducteur_principal_id
            LEFT JOIN LATERAL (SELECT r.ancien_assureur, r.ancien_numero FROM resiliation_hamon r
                               WHERE r.contrat_id = c.id ORDER BY r.id DESC LIMIT 1) rh ON TRUE
            WHERE c.id = ?""";

    /** Options en cours ; à défaut (contrats du sprint 1), celles du devis, sans prix. */
    private static final String OPTIONS = """
            SELECT code_option, prix_mensuel_ttc FROM contrat_option
            WHERE contrat_id = ? AND (date_fin IS NULL OR date_fin >= CURRENT_DATE)
            ORDER BY id""";
    private static final String OPTIONS_DU_DEVIS = """
            SELECT UNNEST(de.options) AS code_option, NULL::NUMERIC AS prix_mensuel_ttc
            FROM contrat c JOIN devis_enregistre de ON de.id = c.devis_id
            WHERE c.id = ?""";

    private final JdbcTemplate jdbc;

    @LogMetier
    public Optional<DonneesContrat> lire(long contratId) {
        List<DonneesContrat> trouves = jdbc.query(CONTRAT, (ligne, rang) -> contrat(ligne, options(contratId)),
                contratId);
        return trouves.stream().findFirst();
    }

    @LogMetier
    public Optional<String> produit(long contratId) {
        return jdbc.queryForList("SELECT produit FROM contrat WHERE id = ?", String.class, contratId).stream()
                .findFirst();
    }

    @LogMetier
    public Optional<String> numero(long contratId) {
        return jdbc.queryForList("SELECT numero FROM contrat WHERE id = ?", String.class, contratId).stream()
                .findFirst();
    }

    /** US-22 : jour de la signature (heure de Paris) ; vide si le contrat n'est pas signé. */
    @LogMetier
    public Optional<LocalDate> dateSignature(long contratId) {
        return jdbc.queryForList("""
                SELECT (signe_le AT TIME ZONE 'Europe/Paris')::DATE FROM contrat
                WHERE id = ? AND signe_le IS NOT NULL""", LocalDate.class, contratId).stream().findFirst();
    }

    private List<DonneesContrat.Option> options(long contratId) {
        List<DonneesContrat.Option> options = jdbc.query(OPTIONS, (ligne, rang) -> option(ligne), contratId);
        return options.isEmpty() ? jdbc.query(OPTIONS_DU_DEVIS, (ligne, rang) -> option(ligne), contratId)
                : options;
    }

    private static DonneesContrat.Option option(ResultSet ligne) throws SQLException {
        return new DonneesContrat.Option(ligne.getString("code_option"), ligne.getBigDecimal("prix_mensuel_ttc"));
    }

    private static DonneesContrat contrat(ResultSet ligne, List<DonneesContrat.Option> options) throws SQLException {
        return new DonneesContrat(
                ligne.getLong("id"),
                ligne.getString("numero"),
                ligne.getString("produit"),
                ligne.getString("formule"),
                new DonneesContrat.Souscripteur(ligne.getString("civilite"), ligne.getString("prenom"),
                        ligne.getString("nom")),
                new DonneesContrat.Logement(ligne.getString("type_logement"), ligne.getObject("surface", Integer.class),
                        ligne.getString("adresse_logement"), ligne.getString("code_postal"), ligne.getString("ville")),
                new DonneesContrat.Vehicule(ligne.getString("immatriculation"), ligne.getString("marque"),
                        ligne.getString("modele"), ligne.getString("version")),
                new DonneesContrat.Conducteur(ligne.getBigDecimal("coefficient_bonus_malus"),
                        ligne.getObject("est_souscripteur", Boolean.class)),
                new DonneesContrat.Tarif(ligne.getBigDecimal("franchise"), ligne.getBigDecimal("cotisation_mensuelle"),
                        ligne.getBigDecimal("cotisation_annuelle"), date(ligne, "date_effet"),
                        date(ligne, "date_fin")),
                new DonneesContrat.Paiement(ligne.getString("frequence_paiement"),
                        ligne.getObject("jour_prelevement", Integer.class), ligne.getString("iban_masque"),
                        ligne.getString("rum")),
                ligne.getString("ancien_assureur") == null ? null
                        : new DonneesContrat.AncienContrat(ligne.getString("ancien_assureur"),
                                ligne.getString("ancien_numero")),
                options);
    }

    private static LocalDate date(ResultSet ligne, String colonne) throws SQLException {
        Date valeur = ligne.getDate(colonne);
        return valeur == null ? null : valeur.toLocalDate();
    }
}
