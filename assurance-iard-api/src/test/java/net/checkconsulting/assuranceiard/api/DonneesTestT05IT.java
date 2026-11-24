package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Sinistre;
import net.checkconsulting.assuranceiard.api.model.Vehicule;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisEnregistreRepository;
import net.checkconsulting.assuranceiard.api.repository.SinistreRepository;
import net.checkconsulting.assuranceiard.api.repository.VehiculeRepository;
import net.checkconsulting.assuranceiard.api.service.DonneesRecetteSprint2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * T-05 sur une vraie base : l'application démarre (Hibernate valide toutes les entités contre les migrations),
 * les données de recette US-04 puis T-05 se chargent sans erreur, une seule fois.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
    "assurance.donnees-test.actif=true",
    "assurance.justificatifs.dossier=target/justificatifs-donnees-test-t05"
})
class DonneesTestT05IT {

    private static final String CLIENT_AUTO = "recette.auto@assureasy.fr";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DonneesRecetteSprint2 donneesT05;

    @Autowired
    private CompteRepository comptes;

    @Autowired
    private DevisEnregistreRepository devis;

    @Autowired
    private VehiculeRepository vehicules;

    @Autowired
    private SinistreRepository sinistres;

    @Test
    void lesDonneesDeTestDuTicketSontChargees() {
        assertThat(compter("SELECT COUNT(DISTINCT statut) FROM contrat WHERE produit = 'AUTO'")).isEqualTo(10);
        assertThat(compter("SELECT COUNT(*) FROM contrat WHERE produit = 'AUTO'"
                + " AND (vehicule_id IS NULL OR conducteur_principal_id IS NULL)")).isZero();
        assertThat(jdbc.queryForList("""
                SELECT d.version || ' ' || d.statut FROM document d JOIN contrat c ON c.id = d.contrat_id
                WHERE c.numero = 'AE-RECETTE-1' AND d.type = 'CONDITIONS_PARTICULIERES' ORDER BY d.version""",
                String.class)).containsExactly("1 REMPLACE", "2 EN_VIGUEUR");
        assertThat(jdbc.queryForList("SELECT DISTINCT statut FROM echeance", String.class))
                .containsExactlyInAnyOrder("PAYEE", "A_VENIR");
        assertThat(compter("SELECT COUNT(*) FROM facture")).isEqualTo(1);
        assertThat(compter("SELECT COUNT(*) FROM avenant a JOIN contrat c ON c.id = a.contrat_id"
                + " WHERE a.statut = 'EN_ATTENTE' AND c.produit = 'HABITATION'")).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT statut FROM sinistre WHERE description LIKE '%recette T-05%'",
                String.class))
                .containsExactlyInAnyOrder("DECLARE", "BROUILLON");
        assertThat(jdbc.queryForList(
                "SELECT statut_devis FROM devis_enregistre WHERE produit = 'AUTO' ORDER BY id", String.class))
                .containsExactly("VEHICULE_RENSEIGNE", "BESOINS_RENSEIGNES", "FORMULE_CHOISIE", "RECAP_VALIDE");
        assertThat(compter("SELECT COUNT(*) FROM compte c WHERE NOT EXISTS"
                + " (SELECT 1 FROM contrat WHERE compte_id = c.id) AND c.profil IN ('PROSPECT', 'ASSURE')"))
                .isGreaterThanOrEqualTo(1);
        // Les 4 comptes de recette d'US-04 ne sont pas dupliqués.
        assertThat(compter("SELECT COUNT(*) FROM compte WHERE email LIKE 'recette.%'")).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT iban_masque FROM mandat_sepa WHERE rum = 'AE-RUM-RECETTE-1'",
                String.class)).isEqualTo("•••• 0189");
    }

    @Test
    void unSecondChargementNeCreeRienEnDouble() {
        int contrats = compter("SELECT COUNT(*) FROM contrat");

        donneesT05.run(null);

        assertThat(compter("SELECT COUNT(*) FROM contrat")).isEqualTo(contrats);
    }

    @Test
    @Transactional
    void lesDevisAutoRestentInvisiblesPourLeCodeHabitationDuSprint1() {
        Compte clientAuto = comptes.findByEmail(CLIENT_AUTO).orElseThrow();

        // US-18 : l'entité lit les deux produits, mais les requêtes de « Mes devis » restent sur l'habitation.
        assertThat(devis.habitationsDuClient(clientAuto)).isEmpty();
        assertThat(devis.findAll()).anyMatch(lu -> "AUTO".equals(lu.getProduit()) && lu.getSurface() == null);
        assertThat(compter("SELECT COUNT(*) FROM devis_enregistre WHERE compte_id = " + clientAuto.getId()))
                .isEqualTo(4);
    }

    @Test
    void lesNouvellesEntitesSEnregistrentAvecLeursValeursParDefaut() {
        Vehicule vehicule = new Vehicule();
        vehicule.setMarque("Renault");
        vehicule.setModele("Zoé");
        vehicule.setEnergie("ELECTRIQUE");
        vehicule.setSource("MARQUE");
        vehicule.setValeurEstimee(new BigDecimal("15000.00"));
        vehicule = vehicules.save(vehicule);

        long contrat = jdbc.queryForObject("SELECT MIN(id) FROM contrat", Long.class);
        Sinistre sinistre = new Sinistre();
        sinistre.setContratId(contrat);
        sinistre.setType("BRIS_GLACE");
        sinistre.setDateSurvenance(LocalDate.now());
        sinistre.setDetails("{\"vitre\": \"pare-brise\"}");
        sinistre = sinistres.save(sinistre);

        assertThat(vehicule.getId()).isNotNull();
        assertThat(vehicule.getDateCreation()).isNotNull();
        assertThat(sinistre.getStatut()).isEqualTo("BROUILLON");
        assertThat(Duration.between(sinistre.getDateCreation(), sinistre.getBrouillonExpireLe()))
                .isEqualTo(Duration.ofDays(30));
        assertThat(jdbc.queryForObject("SELECT details->>'vitre' FROM sinistre WHERE id = ?", String.class,
                sinistre.getId())).isEqualTo("pare-brise");
    }

    private int compter(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
