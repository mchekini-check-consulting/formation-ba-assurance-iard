package net.checkconsulting.assuranceiard.api.service.document;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.service.MandatService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * US-31 étape 5, B9 : modèle PDF du mandat de prélèvement SEPA et de l'échéancier.
 * L'IBAN n'apparaît que masqué (« •••• XXXX »). Pré-notification SEPA incluse.
 */
@Component
@RequiredArgsConstructor
public class ModeleMandatSepaAuto implements ModeleDocument {

    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);
    private static final String FORMAT_MONTANT = "%.2f €";

    private final JdbcTemplate jdbc;
    private final DonneesContratRepository contrats;

    @Override
    public TypeDocument type() {
        return TypeDocument.MANDAT_SEPA;
    }

    @Override
    public boolean convientA(Document document) {
        if (document.getContratId() == null) {
            return false;
        }
        return "AUTO".equals(contrats.produit(document.getContratId()).orElse(""));
    }

    @Override
    public ContenuDocument rediger(Document document) {
        Long contratId = document.getContratId();
        DonneesMandatSepa donnees = chargerDonnees(contratId);
        List<EcheanceData> echeances = chargerEcheances(contratId);

        List<ContenuDocument.Section> sections = new ArrayList<>();

        sections.add(new ContenuDocument.Section("Créancier",
                "Nom : " + MandatService.CREANCIER,
                "Identifiant créancier SEPA (ICS) : " + MandatService.ICS));

        sections.add(new ContenuDocument.Section("Référence unique du mandat (RUM)",
                donnees.rum()));

        sections.add(new ContenuDocument.Section("Débiteur",
                "Titulaire du compte : " + donnees.titulaire(),
                donnees.adresse() != null
                        ? donnees.adresse() + ", " + donnees.codePostal() + " " + donnees.ville()
                        : ""));

        sections.add(new ContenuDocument.Section("Compte bancaire",
                "IBAN : " + donnees.ibanMasque(),
                "BIC : " + donnees.bic() + (donnees.banque() != null ? " (" + donnees.banque() + ")" : "")));

        sections.add(new ContenuDocument.Section("Type de paiement",
                "Récurrent, mensuel",
                "Jour de prélèvement : le " + donnees.jourPrelevement() + " du mois",
                "Date de signature : " + (donnees.dateSignature() != null
                        ? donnees.dateSignature().format(FORMAT_DATE) : "—")));

        sections.add(new ContenuDocument.Section("Droits du débiteur",
                "Remboursement possible d'un prélèvement autorisé sous 8 semaines.",
                "Remboursement possible sous 13 mois si le prélèvement n'a pas été autorisé."));

        sections.add(sectionEcheancier(echeances, donnees.prixMensuelTtc()));

        return new ContenuDocument("Mandat de prélèvement SEPA et échéancier",
                donnees.numero(), sections);
    }

    private ContenuDocument.Section sectionEcheancier(List<EcheanceData> echeances, BigDecimal montant) {
        List<String> lignes = new ArrayList<>();
        for (EcheanceData e : echeances) {
            lignes.add(e.date().format(FORMAT_DATE)
                    + " – " + String.format(Locale.FRANCE, FORMAT_MONTANT, e.montantTtc()));
        }
        lignes.add("");
        lignes.add("Total annuel : "
                + String.format(Locale.FRANCE, FORMAT_MONTANT,
                        montant.multiply(BigDecimal.valueOf(echeances.size()))));
        lignes.add("Ce document vaut pré-notification des prélèvements SEPA.");
        lignes.add("Simplification du prototype : aucun prorata pour le premier prélèvement.");
        return new ContenuDocument.Section("Échéancier des prélèvements", lignes);
    }

    private DonneesMandatSepa chargerDonnees(Long contratId) {
        return jdbc.queryForObject("""
                SELECT c.numero, c.cotisation_mensuelle,
                       m.rum, m.iban_masque, m.titulaire, m.bic, m.banque,
                       m.date_signature::DATE, m.jour_prelevement,
                       sa.adresse, sa.code_postal, sa.ville
                FROM contrat c
                JOIN mandat_sepa m ON m.id = c.mandat_sepa_id
                LEFT JOIN devis_auto da ON da.id = c.devis_auto_id
                LEFT JOIN souscripteur_auto sa ON sa.devis_auto_id = da.id
                WHERE c.id = ?""",
                (rs, row) -> mapper(rs), contratId);
    }

    private List<EcheanceData> chargerEcheances(Long contratId) {
        return jdbc.query(
                "SELECT date_echeance, montant_ttc FROM echeance WHERE contrat_id = ? ORDER BY date_echeance ASC",
                (rs, row) -> new EcheanceData(toLocalDate(rs, "date_echeance"), rs.getBigDecimal("montant_ttc")),
                contratId);
    }

    private static DonneesMandatSepa mapper(ResultSet rs) throws SQLException {
        return new DonneesMandatSepa(
                rs.getString("numero"),
                rs.getBigDecimal("cotisation_mensuelle"),
                rs.getString("rum"),
                rs.getString("iban_masque"),
                rs.getString("titulaire"),
                rs.getString("bic"),
                rs.getString("banque"),
                toLocalDate(rs, "date_signature"),
                rs.getObject("jour_prelevement", Integer.class),
                rs.getString("adresse"),
                rs.getString("code_postal"),
                rs.getString("ville"));
    }

    private static LocalDate toLocalDate(ResultSet rs, String col) throws SQLException {
        Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    private record DonneesMandatSepa(
            String numero, BigDecimal prixMensuelTtc,
            String rum, String ibanMasque, String titulaire, String bic, String banque,
            LocalDate dateSignature, Integer jourPrelevement,
            String adresse, String codePostal, String ville) {
    }

    private record EcheanceData(LocalDate date, BigDecimal montantTtc) {
    }
}
