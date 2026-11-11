package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.model.AccesRefuse;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.Justificatif;
import net.checkconsulting.assuranceiard.api.model.Profil;
import net.checkconsulting.assuranceiard.api.repository.AccesRefuseRepository;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.JustificatifRepository;
import net.checkconsulting.assuranceiard.api.securite.MotifRefus;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * US-04 sur une vraie base, avec les données de recette (RG5) : 2 assurés avec chacun un devis, un contrat et un
 * document, un gestionnaire et un administrateur.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "assurance.donnees-test.actif=true")
@AutoConfigureMockMvc
class ControleAccesIT {

    private static final String ACCES_NON_AUTORISE = "Accès non autorisé.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompteRepository comptes;

    @Autowired
    private ContratRepository contrats;

    @Autowired
    private JustificatifRepository justificatifs;

    @Autowired
    private AccesRefuseRepository journal;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private TransactionTemplate transaction;

    private String assureA;
    private String assureB;
    private String gestionnaire;
    private String administrateur;
    private long idA;
    private long idB;
    private UUID devisB;
    private long documentB;

    @BeforeEach
    void connecter() {
        assureA = jeton("recette.assure1@assureasy.fr");
        assureB = jeton("recette.assure2@assureasy.fr");
        gestionnaire = jeton("recette.gestionnaire@assureasy.fr");
        administrateur = jeton("recette.admin@assureasy.fr");
        transaction.executeWithoutResult(etat -> {
            Compte b = comptes.findByEmail("recette.assure2@assureasy.fr").orElseThrow();
            idA = comptes.findByEmail("recette.assure1@assureasy.fr").orElseThrow().getId();
            idB = b.getId();
            Contrat contrat = contrats.findByDossierCompteOrderByDateCreationDesc(b).get(0);
            devisB = contrat.getDossier().getReferenceDevis();
            Justificatif document = justificatifs.findByDossierCompteOrderByIdDesc(b).get(0);
            documentB = document.getId();
        });
    }

    private String jeton(String email) {
        return transaction.execute(etat ->
                "Bearer " + sessionService.ouvrir(comptes.findByEmail(email).orElseThrow()).jeton());
    }

    private ResultActions lire(String jeton, String chemin, Object... variables) throws Exception {
        return mockMvc.perform(get(chemin, variables).header(HttpHeaders.AUTHORIZATION, jeton));
    }

    private List<AccesRefuse> refusDe(long compteId) {
        return journal.findAll(Sort.by("id")).stream()
                .filter(ligne -> Long.valueOf(compteId).equals(ligne.getCompteId()))
                .toList();
    }

    @Test
    void ac1LAssureNeVoitQueSesDevisContratsEtDocuments() throws Exception {
        lire(assureA, "/devis/enregistres")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ville").value("Paris"));
        lire(assureA, "/contrats")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numero").value("AE-RECETTE-1"));
        lire(assureA, "/documents")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        lire(assureB, "/contrats")
                .andExpect(jsonPath("$[0].numero").value("AE-RECETTE-2"));
    }

    @Test
    void ac2LAssureAQuiDemandeLesDonneesDeBEstRefuseEtLaTentativeJournalisee() throws Exception {
        int avant = refusDe(idA).size();

        lire(assureA, "/devis/contrat/{ref}", devisB)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(ACCES_NON_AUTORISE))
                .andExpect(jsonPath("$.numero").doesNotExist());
        mockMvc.perform(post("/devis/enregistres/{ref}/reprise", devisB).header(HttpHeaders.AUTHORIZATION, assureA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.formule").doesNotExist());
        mockMvc.perform(delete("/devis/enregistres/{ref}", devisB).header(HttpHeaders.AUTHORIZATION, assureA))
                .andExpect(status().isForbidden());
        lire(assureA, "/documents/{id}", documentB)
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        List<AccesRefuse> refus = refusDe(idA);
        assertThat(refus).hasSize(avant + 4);
        refus = refus.subList(avant, refus.size());
        assertThat(refus).allSatisfy(ligne -> {
            assertThat(ligne.getMotif()).isEqualTo(MotifRefus.PAS_PROPRIETAIRE);
            assertThat(ligne.getEmail()).isEqualTo("recette.assure1@assureasy.fr");
            assertThat(ligne.getDateTentative()).isNotNull();
        });
        assertThat(refus).extracting(AccesRefuse::getIdentifiant)
                .contains(devisB.toString(), String.valueOf(documentB));

        // Le devis de B n'a pas été supprimé, et B ouvre toujours son document
        lire(assureB, "/devis/enregistres").andExpect(jsonPath("$.length()").value(1));
        lire(assureB, "/documents/{id}", documentB)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void ac3UnAssureNAccedeAAucuneAdresseDuBackOffice() throws Exception {
        int avant = refusDe(idA).size();

        for (String chemin : List.of("/back-office/clients", "/back-office/clients/" + idB, "/admin/profils",
                "/admin/profils/ASSURE/historique", "/admin/acces-refuses")) {
            lire(assureA, chemin)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value(ACCES_NON_AUTORISE));
        }
        mockMvc.perform(put("/admin/profils/ASSURE/permissions")
                        .header(HttpHeaders.AUTHORIZATION, assureA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[]}"))
                .andExpect(status().isForbidden());

        List<AccesRefuse> refus = refusDe(idA);
        assertThat(refus).hasSize(avant + 6);
        assertThat(refus.subList(avant, refus.size()))
                .extracting(AccesRefuse::getMotif).containsOnly(MotifRefus.BACK_OFFICE);
    }

    @Test
    void ac3LesDroitsDuProfilSontFournisParLeServeur() throws Exception {
        lire(assureA, "/session")
                .andExpect(jsonPath("$.profil").value("ASSURE"))
                .andExpect(jsonPath("$.droits").value(Matchers.hasItems("CONTRATS:CONSULTER", "DEVIS:CONSULTER")))
                .andExpect(jsonPath("$.droits").value(Matchers.not(Matchers.hasItem("PROFILS:CONSULTER"))));
    }

    @Test
    void ac4EtRg4LIbanEstMasquePourChaqueProfil() throws Exception {
        UUID devisA = transaction.execute(etat -> contrats.findByDossierCompteOrderByDateCreationDesc(
                comptes.findById(idA).orElseThrow()).get(0).getDossier().getReferenceDevis());
        String contratDeA = lire(assureA, "/devis/contrat/{ref}", devisA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ibanFin").value("0189"))
                .andExpect(jsonPath("$.ibanDebut").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(contratDeA).doesNotContain("FR76");

        for (String jeton : List.of(gestionnaire, administrateur)) {
            String dossier = lire(jeton, "/back-office/clients/{id}", idB)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contrats[0].ibanFin").value("4321"))
                    .andExpect(jsonPath("$.contrats[0].titulaire").value("Bruno Petit"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(dossier).doesNotContain("FR76");
        }
    }

    @Test
    void leGestionnaireConsulteLesDossiersClientsMaisPasLaGestionDesProfils() throws Exception {
        lire(gestionnaire, "/back-office/clients")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email").value(Matchers.hasItems(
                        "recette.assure1@assureasy.fr", "recette.assure2@assureasy.fr")))
                .andExpect(jsonPath("$[*].email").value(Matchers.not(Matchers.hasItem(
                        "recette.admin@assureasy.fr"))));
        lire(gestionnaire, "/admin/profils").andExpect(status().isForbidden());
        lire(gestionnaire, "/admin/acces-refuses").andExpect(status().isForbidden());
    }

    @Test
    void rg3LAdministrateurConsulteLeJournalDesAccesRefuses() throws Exception {
        lire(assureA, "/documents/{id}", documentB).andExpect(status().isForbidden());

        lire(administrateur, "/admin/acces-refuses")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("recette.assure1@assureasy.fr"))
                .andExpect(jsonPath("$[0].ressource").value("DOCUMENT"))
                .andExpect(jsonPath("$[0].identifiant").value(String.valueOf(documentB)))
                .andExpect(jsonPath("$[0].droit").value("CONSULTER"))
                .andExpect(jsonPath("$[0].motif").value("PAS_PROPRIETAIRE"));
    }

    @Test
    void ac5UnDroitRetireParLAdministrateurEstRefuseALaRequeteSuivante() throws Exception {
        lire(assureA, "/contrats").andExpect(status().isOk());
        try {
            changerLeDroit("CONTRATS", "CONSULTER", false);

            lire(assureA, "/contrats")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value(ACCES_NON_AUTORISE));
            lire(assureA, "/session")
                    .andExpect(jsonPath("$.droits").value(Matchers.not(Matchers.hasItem("CONTRATS:CONSULTER"))));
            assertThat(refusDe(idA)).last()
                    .extracting(AccesRefuse::getMotif).isEqualTo(MotifRefus.DROIT_ABSENT);
        } finally {
            changerLeDroit("CONTRATS", "CONSULTER", true);
        }
        lire(assureA, "/contrats").andExpect(status().isOk());
    }

    @Test
    void lesDonneesDeRecetteOntLesBonsProfils() {
        assertThat(comptes.findById(idA)).get().extracting(Compte::getProfil).isEqualTo(Profil.ASSURE);
        assertThat(comptes.existsByEmail("recette.gestionnaire@assureasy.fr")).isTrue();
        assertThat(comptes.existsByEmail("recette.admin@assureasy.fr")).isTrue();
    }

    private void changerLeDroit(String rubrique, String droit, boolean valeur) throws Exception {
        mockMvc.perform(put("/admin/profils/ASSURE/permissions")
                        .header(HttpHeaders.AUTHORIZATION, administrateur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissions":[{"rubrique":"%s","droit":"%s","valeur":%s}]}
                                """.formatted(rubrique, droit, valeur)))
                .andExpect(status().isNoContent());
    }
}
