package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Profil;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.service.EnvoiEmailContrat;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** US-33 : signature d'un contrat auto — parcours complet sur une vraie base. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ContratAutoIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompteRepository comptes;

    @Autowired
    private ContratRepository contratsRepo;

    @Autowired
    private DocumentRepository documents;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private EnvoiEmailContrat envoiEmail;

    private String jeton;
    private long devisAutoId;
    private long contratId;
    private Compte compte;

    @BeforeEach
    void preparer() {
        compte = new Compte();
        compte.setEmail(UUID.randomUUID() + "@auto-test.fr");
        compte.setMotDePasseHash("hash");
        compte.setCivilite("M.");
        compte.setPrenom("Jean");
        compte.setNom("Dupont");
        compte.setProfil(Profil.PROSPECT);
        compte = comptes.save(compte);
        jeton = "Bearer " + sessionService.ouvrir(compte).jeton();

        long vehiculeId = jdbc.queryForObject("""
                INSERT INTO vehicule (marque, modele, energie, source, immatriculation)
                VALUES ('Renault', 'Clio', 'ESSENCE', 'PLAQUE', 'AB-123-CD')
                RETURNING id""", Long.class);

        long conducteurId = jdbc.queryForObject(
                "INSERT INTO conducteur DEFAULT VALUES RETURNING id", Long.class);

        long devisEnregistreId = jdbc.queryForObject("""
                INSERT INTO devis_enregistre (compte_id, produit, statut_devis, date_creation,
                                              date_fin_validite, date_dernier_contact)
                VALUES (?, 'AUTO', 'RECAP_VALIDE', NOW(), CURRENT_DATE + 30, NOW())
                RETURNING id""", Long.class, compte.getId());

        devisAutoId = jdbc.queryForObject("""
                INSERT INTO devis_auto (devis_id, vehicule_id, conducteur_id, assure_plus_un_an,
                                        date_effet_souhaitee)
                VALUES (?, ?, ?, FALSE, CURRENT_DATE + 30)
                RETURNING id""", Long.class, devisEnregistreId, vehiculeId, conducteurId);

        long mandatId = jdbc.queryForObject("""
                INSERT INTO mandat_sepa (compte_id, rum, iban_debut, iban_fin, jour_prelevement,
                                         date_creation)
                VALUES (?, ?, 'FR76', '0189', 5, NOW())
                RETURNING id""", Long.class, compte.getId(), "AE-RUM-" + UUID.randomUUID().toString().substring(0, 8));

        contratId = jdbc.queryForObject("""
                INSERT INTO contrat (numero, produit, compte_id, statut, formule, franchise,
                                     cotisation_mensuelle, cotisation_annuelle, date_effet,
                                     vehicule_id, conducteur_principal_id, mandat_sepa_id,
                                     devis_auto_id, date_creation)
                VALUES (?, 'AUTO', ?, 'EN_ATTENTE_DE_SIGNATURE', 'TIERS', 300.00, 42.50, 510.00,
                        CURRENT_DATE + 30, ?, ?, ?, ?, NOW())
                RETURNING id""", Long.class,
                "AUTO-IT-" + UUID.randomUUID().toString().substring(0, 8),
                compte.getId(), vehiculeId, conducteurId, mandatId, devisAutoId);
    }

    @Test
    void acGetChargeLeContratAvecToutesLesDonnees() throws Exception {
        mockMvc.perform(get("/v1/devis/auto/{id}/contrat/signature", devisAutoId)
                        .header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_DE_SIGNATURE"))
                .andExpect(jsonPath("$.prenom").value("Jean"))
                .andExpect(jsonPath("$.formule").value("TIERS"))
                .andExpect(jsonPath("$.vehiculeImmatriculation").value("AB-123-CD"))
                .andExpect(jsonPath("$.justificatifsAttendus").isArray());
    }

    @Test
    void acGetRefuseSiCompteNonProprietaire() throws Exception {
        Compte autre = new Compte();
        autre.setEmail(UUID.randomUUID() + "@autre.fr");
        autre.setMotDePasseHash("hash");
        autre = comptes.save(autre);
        String jetonAutre = "Bearer " + sessionService.ouvrir(autre).jeton();

        mockMvc.perform(get("/v1/devis/auto/{id}/contrat/signature", devisAutoId)
                        .header(HttpHeaders.AUTHORIZATION, jetonAutre))
                .andExpect(status().isForbidden());
    }

    @Test
    void acGetSansSessionEst401() throws Exception {
        mockMvc.perform(get("/v1/devis/auto/{id}/contrat/signature", devisAutoId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acSignatureCompleteDateFutureTransitionEnAttenteEffet() throws Exception {
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

        // Envoi du code
        mockMvc.perform(post("/v1/devis/auto/{id}/contrat/signature/code", devisAutoId)
                        .header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailMasque").exists());

        verify(envoiEmail).envoyer(anyString(), codeCaptor.capture());
        String code = codeCaptor.getValue();

        // Signature avec le bon code
        mockMvc.perform(post("/v1/devis/auto/{id}/contrat/signature", devisAutoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, jeton)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_EFFET"));

        // Vérifications en base
        assertThat(contratsRepo.findById(contratId))
                .isPresent()
                .hasValueSatisfying(c -> {
                    assertThat(c.getStatut()).isEqualTo("EN_ATTENTE_EFFET");
                    assertThat(c.getSigneLe()).isNotNull();
                });

        // Profil promu
        assertThat(comptes.findById(compte.getId()))
                .isPresent()
                .hasValueSatisfying(c -> assertThat(c.getProfil()).isEqualTo(Profil.ASSURE));

        // Conditions particulières créées
        assertThat(documents.enVigueur(contratId, "CONDITIONS_PARTICULIERES", ""))
                .isPresent()
                .hasValueSatisfying(cp -> {
                    assertThat(cp.getVersion()).isEqualTo(1);
                    assertThat(cp.getStocke()).isTrue();
                });

        // Mémo véhicule créé
        assertThat(documents.enVigueur(contratId, "MEMO_VEHICULE", ""))
                .isPresent()
                .hasValueSatisfying(memo -> {
                    assertThat(memo.getVersion()).isEqualTo(1);
                    assertThat(memo.getStocke()).isTrue();
                });
    }

    @Test
    void acMauvaisCodeRetourne422AvecEssaisRestants() throws Exception {
        envoyerCode();

        soumettreCode("999999")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.essaisRestants").value(2));
    }

    @Test
    void acTroisMauvaisCodesBloquentLaSignature() throws Exception {
        envoyerCode();

        soumettreCode("000001").andExpect(status().isUnprocessableEntity());
        soumettreCode("000002").andExpect(status().isUnprocessableEntity());
        soumettreCode("000003").andExpect(status().isConflict())
                .andExpect(jsonPath("$.motif").value("essais-epuises"));
    }

    @Test
    void acRenvoyerCodeReinitialiseLeCompteur() throws Exception {
        envoyerCode();
        soumettreCode("000001").andExpect(status().isUnprocessableEntity());
        soumettreCode("000002").andExpect(status().isUnprocessableEntity());

        // Renvoyer un code
        mockMvc.perform(post("/v1/devis/auto/{id}/contrat/signature/code/renvoyer", devisAutoId)
                        .header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk());

        // Toujours 2 essais restants avec un mauvais code après le renvoi
        soumettreCode("000003")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.essaisRestants").value(2));
    }

    private ResultActions envoyerCode() throws Exception {
        return mockMvc.perform(post("/v1/devis/auto/{id}/contrat/signature/code", devisAutoId)
                        .header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk());
    }

    private ResultActions soumettreCode(String code) throws Exception {
        return mockMvc.perform(post("/v1/devis/auto/{id}/contrat/signature", devisAutoId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, jeton)
                .content("{\"code\":\"" + code + "\"}"));
    }
}
