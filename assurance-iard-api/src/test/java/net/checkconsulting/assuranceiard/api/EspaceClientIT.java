package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.model.AccesRefuse;
import net.checkconsulting.assuranceiard.api.repository.AccesRefuseRepository;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.securite.MotifRefus;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import net.checkconsulting.assuranceiard.api.service.document.RepriseDocuments;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * US-18 sur une vraie base, avec les données de recette d'US-04 et de T-05 : Alice (habitation AE-RECETTE-1),
 * Camille (10 contrats auto, un par statut, et l'habitation AE-RECETTE-H01), David (prospect sans contrat).
 * AC7 : chacun ne voit que ses données, et tout accès au contrat d'un autre est refusé et tracé.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
    "assurance.donnees-test.actif=true",
    "assurance.justificatifs.dossier=target/justificatifs-espace-client"
})
@AutoConfigureMockMvc
class EspaceClientIT {

    private static final String ALICE = "recette.assure1@assureasy.fr";
    private static final String CAMILLE = "recette.auto@assureasy.fr";
    private static final String DAVID = "recette.prospect@assureasy.fr";
    private static final String ACCES_NON_AUTORISE = "Accès non autorisé.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CompteRepository comptes;

    @Autowired
    private AccesRefuseRepository journal;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private RepriseDocuments reprise;

    @Autowired
    private TransactionTemplate transaction;

    private String alice;
    private String camille;
    private String david;

    @BeforeEach
    void connecter() {
        alice = jeton(ALICE);
        camille = jeton(CAMILLE);
        david = jeton(DAVID);
    }

    private String jeton(String email) {
        return transaction.execute(etat ->
                "Bearer " + sessionService.ouvrir(comptes.findByEmail(email).orElseThrow()).jeton());
    }

    private ResultActions lire(String jeton, String chemin, Object... variables) throws Exception {
        return mockMvc.perform(get(chemin, variables).header(HttpHeaders.AUTHORIZATION, jeton));
    }

    private long id(String email) {
        return transaction.execute(etat -> comptes.findByEmail(email).orElseThrow().getId());
    }

    private List<AccesRefuse> refusDe(String email) {
        long compte = id(email);
        return journal.findAll(Sort.by("id")).stream()
                .filter(ligne -> Long.valueOf(compte).equals(ligne.getCompteId()))
                .toList();
    }

    private long document(String numeroContrat, String type) {
        return jdbc.queryForObject("""
                SELECT d.id FROM document d JOIN contrat c ON c.id = d.contrat_id
                WHERE c.numero = ? AND d.type = ? AND d.statut = 'EN_VIGUEUR'""", Long.class, numeroContrat, type);
    }

    @Test
    void ac1TableauDeBordDAlice() throws Exception {
        lire(alice, "/espace-client/tableau-de-bord")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prenom").value("Alice"))
                .andExpect(jsonPath("$.contrats.length()").value(1))
                .andExpect(jsonPath("$.contrats[0].numero").value("AE-RECETTE-1"))
                .andExpect(jsonPath("$.contrats[0].produit").value("HABITATION"))
                .andExpect(jsonPath("$.contrats[0].dateEcheance").isNotEmpty())
                .andExpect(jsonPath("$.dernieresFactures[0].numero").value("FAC-RECETTE-0001"))
                .andExpect(jsonPath("$.dernieresFactures[0].numeroContrat").value("AE-RECETTE-1"))
                // Aucun modèle ne produit encore le PDF d'une facture : pas de lien de téléchargement.
                .andExpect(jsonPath("$.dernieresFactures[0].documentId").doesNotExist())
                .andExpect(jsonPath("$.sinistres.length()").value(1))
                .andExpect(jsonPath("$.sinistres[0].numero").value("SIN-RECETTE-0001"))
                // Son devis habitation est transformé en contrat : il n'est plus « en cours ».
                .andExpect(jsonPath("$.devisEnCours").isEmpty());
    }

    @Test
    void ac1TableauDeBordDeCamilleHabitationEtAuto() throws Exception {
        lire(camille, "/espace-client/tableau-de-bord")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prenom").value("Camille"))
                .andExpect(jsonPath("$.contrats.length()").value(11))
                .andExpect(jsonPath("$.contrats[*].statut").value(hasItem("RENONCE")))
                .andExpect(jsonPath("$.contrats[*].numero").value(hasItem("AE-RECETTE-H01")))
                // 4 devis auto, dont 2 seulement avec une formule et un prix.
                .andExpect(jsonPath("$.devisEnCours.length()").value(2))
                .andExpect(jsonPath("$.devisEnCours[*].produit").value(containsInAnyOrder("AUTO", "AUTO")))
                .andExpect(jsonPath("$.devisEnCours[*].description")
                        .value(containsInAnyOrder("Peugeot 208 · EF-003-GH", "Peugeot 208 · EF-004-GH")))
                // Son seul sinistre est un brouillon : non affiché.
                .andExpect(jsonPath("$.sinistres").isEmpty())
                .andExpect(jsonPath("$.dernieresFactures").isEmpty())
                .andExpect(jsonPath("$.contrats[*].numero").value(not(hasItem("AE-RECETTE-1"))));
    }

    @Test
    void ac6LeProspectSansContratAccedeASonTableauDeBordSansRefus() throws Exception {
        int avant = refusDe(DAVID).size();

        lire(david, "/espace-client/tableau-de-bord")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prenom").value("David"))
                .andExpect(jsonPath("$.contrats").isEmpty())
                .andExpect(jsonPath("$.devisVisibles").value(true))
                .andExpect(jsonPath("$.sinistresVisibles").value(false));

        assertThat(refusDe(DAVID)).hasSize(avant);
    }

    @Test
    void sansSessionLeTableauDeBordRepond401() throws Exception {
        mockMvc.perform(get("/espace-client/tableau-de-bord")).andExpect(status().isUnauthorized());
        lire("Bearer inconnu", "/espace-client/tableau-de-bord").andExpect(status().isUnauthorized());
    }

    @Test
    void us18MesContratsRetrouveLesContratsParLeCompte() throws Exception {
        lire(alice, "/contrats")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].referenceDevis").isNotEmpty());
        lire(camille, "/contrats")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11))
                .andExpect(jsonPath("$[?(@.numero == 'AE-RECETTE-A03')].bienAssure")
                        .value(hasItem("Renault Clio · AB-003-CD")));
    }

    @Test
    void ac7AliceEtCamilleNeVoientJamaisLesContratsDeLAutreEtLesRefusSontTraces() throws Exception {
        int avantAlice = refusDe(ALICE).size();
        int avantCamille = refusDe(CAMILLE).size();
        long cpDeCamille = document("AE-RECETTE-A03", "CONDITIONS_PARTICULIERES");
        long cpDAlice = document("AE-RECETTE-1", "CONDITIONS_PARTICULIERES");

        lire(alice, "/contrats/{numero}", "AE-RECETTE-A03")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(ACCES_NON_AUTORISE))
                .andExpect(jsonPath("$.vehicule").doesNotExist());
        lire(alice, "/documents-contrat/{id}", cpDeCamille).andExpect(status().isForbidden());
        lire(camille, "/contrats/{numero}", "AE-RECETTE-1")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.logement").doesNotExist());
        lire(camille, "/documents-contrat/{id}", cpDAlice).andExpect(status().isForbidden());

        List<AccesRefuse> refusAlice = refusDe(ALICE);
        assertThat(refusAlice).hasSize(avantAlice + 2);
        assertThat(refusAlice.subList(avantAlice, refusAlice.size())).allSatisfy(ligne -> {
            assertThat(ligne.getMotif()).isEqualTo(MotifRefus.PAS_PROPRIETAIRE);
            assertThat(ligne.getEmail()).isEqualTo(ALICE);
        }).extracting(AccesRefuse::getIdentifiant).containsExactly("AE-RECETTE-A03", String.valueOf(cpDeCamille));
        List<AccesRefuse> refusCamille = refusDe(CAMILLE);
        assertThat(refusCamille).hasSize(avantCamille + 2);
        assertThat(refusCamille.subList(avantCamille, refusCamille.size()))
                .extracting(AccesRefuse::getIdentifiant).containsExactly("AE-RECETTE-1", String.valueOf(cpDAlice));
    }

    @Test
    void ac7LAncienneAdresseParReferenceDeDevisResteProtegee() throws Exception {
        UUID devisDAlice = jdbc.queryForObject("""
                SELECT d.reference_devis FROM contrat c JOIN dossier_souscription d ON d.id = c.dossier_id
                WHERE c.numero = 'AE-RECETTE-1'""", UUID.class);
        int avant = refusDe(CAMILLE).size();

        // Ce que l'écran utilise pour rediriger une ancienne adresse vers la fiche par numéro.
        lire(alice, "/devis/contrat/{ref}", devisDAlice)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("AE-RECETTE-1"));
        lire(camille, "/devis/contrat/{ref}", devisDAlice)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.numero").doesNotExist());

        assertThat(refusDe(CAMILLE)).hasSize(avant + 1);
    }

    @Test
    void unNumeroInconnuRepondIntrouvableEtUnProspectNAPasAccesAuContratDUnAutre() throws Exception {
        lire(camille, "/contrats/{numero}", "AE-INCONNU").andExpect(status().isNotFound());
        // Le prospect a le droit de consulter ses contrats (US-04, V18), pas ceux des autres.
        lire(david, "/contrats/{numero}", "AE-RECETTE-1").andExpect(status().isForbidden());
        assertThat(refusDe(DAVID)).last().extracting(AccesRefuse::getMotif).isEqualTo(MotifRefus.PAS_PROPRIETAIRE);
    }

    @Test
    void ac3FicheAutoPuisTelechargementDesConditionsParticulieres() throws Exception {
        long conditionsParticulieres = document("AE-RECETTE-A03", "CONDITIONS_PARTICULIERES");
        jdbc.update("UPDATE document SET stocke = FALSE WHERE id = ?", conditionsParticulieres);

        // Juste après le démarrage, le PDF n'est pas encore stocké : 409 (l'écran affiche « en préparation »).
        lire(camille, "/documents-contrat/{id}", conditionsParticulieres)
                .andExpect(status().isConflict());

        reprise.reprendre();

        String fiche = lire(camille, "/contrats/{numero}", "AE-RECETTE-A03")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produit").value("AUTO"))
                .andExpect(jsonPath("$.statut").value("ACTIF"))
                .andExpect(jsonPath("$.vehicule.marque").value("Renault"))
                .andExpect(jsonPath("$.vehicule.immatriculation").value("AB-003-CD"))
                .andExpect(jsonPath("$.logement").doesNotExist())
                .andExpect(jsonPath("$.garanties[0].code").value("RC"))
                .andExpect(jsonPath("$.paiement.ibanMasque").value("•••• 7788"))
                .andExpect(jsonPath("$.documents[*].type")
                        .value(containsInAnyOrder("CONDITIONS_PARTICULIERES", "CONDITIONS_GENERALES")))
                .andReturn().getResponse().getContentAsString();
        // RGPD : ni IBAN complet, ni données du conducteur.
        assertThat(fiche).doesNotContain("FR76", "bonus", "dateNaissance", "1988");

        lire(camille, "/documents-contrat/{id}", conditionsParticulieres)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void ac3LaFicheNAfficheQueLaVersionEnVigueurDesConditionsParticulieres() throws Exception {
        lire(alice, "/contrats/{numero}", "AE-RECETTE-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logement.ville").value("Paris"))
                .andExpect(jsonPath("$.vehicule").doesNotExist())
                .andExpect(jsonPath("$.documents[?(@.type == 'CONDITIONS_PARTICULIERES')].version").value(hasItem(2)))
                .andExpect(jsonPath("$.documents[?(@.type == 'CONDITIONS_PARTICULIERES')].version")
                        .value(not(hasItem(1))));
    }

    @Test
    void nonRegressionLesContratsDuSprint1SontToujoursRattachesAuBonClient() {
        // Les contrats de recette d'US-04 sont créés par l'entité Contrat, comme à la souscription habitation.
        List<Map<String, Object>> lignes = jdbc.queryForList("""
                SELECT c.produit, c.formule, c.compte_id = d.compte_id AS meme_client
                FROM contrat c JOIN dossier_souscription d ON d.id = c.dossier_id""");

        assertThat(lignes).isNotEmpty().allSatisfy(ligne -> {
            assertThat(ligne.get("produit")).isEqualTo("HABITATION");
            assertThat(ligne.get("formule")).isNotNull();
            assertThat(ligne.get("meme_client")).isEqualTo(Boolean.TRUE);
        });
    }

    @Test
    void nonRegressionMesDevisResteSurLHabitation() throws Exception {
        lire(alice, "/devis/enregistres")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        // Camille n'a que des devis auto : la liste du sprint 1 ne les montre pas, sans erreur.
        lire(camille, "/devis/enregistres")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
