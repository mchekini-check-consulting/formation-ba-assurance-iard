package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import net.checkconsulting.assuranceiard.api.service.document.DemandeDocument;
import net.checkconsulting.assuranceiard.api.service.document.DocumentsGeneresService;
import net.checkconsulting.assuranceiard.api.service.document.GabaritPdf;
import net.checkconsulting.assuranceiard.api.service.document.RepriseDocuments;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * T-03, critères « Terminé quand », sur une vraie base avec les données de test T-05 : reprise (conditions
 * particulières du sprint 1 et de recette, conditions générales), versions, échec non bloquant, téléchargement
 * réservé au propriétaire (refus tracé), empreinte vérifiée.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
    "assurance.donnees-test.actif=true",
    "assurance.justificatifs.dossier=target/justificatifs-documents-generes"
})
@AutoConfigureMockMvc
class DocumentsGeneresIT {

    private static final String CLIENT_AUTO = "recette.auto@assureasy.fr";
    private static final String CLIENT_1 = "recette.assure1@assureasy.fr";
    private static final String GESTIONNAIRE = "recette.gestionnaire@assureasy.fr";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RepriseDocuments reprise;

    @Autowired
    private DocumentsGeneresService service;

    @Autowired
    private CompteRepository comptes;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private TransactionTemplate transaction;

    @BeforeEach
    void reprendre() {
        reprise.reprendre();
    }

    private String jeton(String email) {
        return transaction.execute(etat ->
                "Bearer " + sessionService.ouvrir(comptes.findByEmail(email).orElseThrow()).jeton());
    }

    private ResultActions telecharger(String email, long document) throws Exception {
        return mockMvc.perform(get("/documents-contrat/{id}", document).header(HttpHeaders.AUTHORIZATION,
                jeton(email)));
    }

    private long document(String numeroContrat, String statut) {
        return jdbc.queryForObject("""
                SELECT d.id FROM document d JOIN contrat c ON c.id = d.contrat_id
                WHERE c.numero = ? AND d.type = 'CONDITIONS_PARTICULIERES' AND d.statut = ?""", Long.class,
                numeroContrat, statut);
    }

    private long conditionsGenerales(String produit) {
        return jdbc.queryForObject("""
                SELECT id FROM document WHERE type = 'CONDITIONS_GENERALES' AND reference = ?
                AND statut = 'EN_VIGUEUR'""", Long.class, produit);
    }

    private static String texte(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    @Test
    void laRepriseStockeLesConditionsParticulieresEnVigueurSansCreerDeVersion() throws Exception {
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM document
                WHERE type = 'CONDITIONS_PARTICULIERES' AND statut = 'EN_VIGUEUR' AND NOT stocke""", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM document d JOIN document_contenu dc ON dc.document_id = d.id
                WHERE d.type = 'CONDITIONS_PARTICULIERES' AND d.cle_stockage = 'base:document_contenu/' || d.id
                  AND d.statut = 'EN_VIGUEUR' AND LENGTH(d.empreinte) = 64""", Integer.class))
                .isEqualTo(jdbc.queryForObject("""
                        SELECT COUNT(*) FROM document
                        WHERE type = 'CONDITIONS_PARTICULIERES' AND statut = 'EN_VIGUEUR'""", Integer.class))
                .isGreaterThanOrEqualTo(11);
        // Données de recette T-05 : V1 REMPLACE jamais stockée (pas reconstituée), V2 EN_VIGUEUR stockée.
        assertThat(jdbc.queryForList("""
                SELECT d.version || ' ' || d.statut || ' ' || d.stocke FROM document d
                JOIN contrat c ON c.id = d.contrat_id
                WHERE c.numero = 'AE-RECETTE-1' AND d.type = 'CONDITIONS_PARTICULIERES' ORDER BY d.version""",
                String.class)).containsExactly("1 REMPLACE false", "2 EN_VIGUEUR true");
        // Conditions générales provisoires, une par produit, créées une seule fois.
        assertThat(jdbc.queryForList("""
                SELECT reference || ' ' || version || ' ' || stocke FROM document
                WHERE type = 'CONDITIONS_GENERALES' ORDER BY reference""", String.class))
                .containsExactly("AUTO 1 true", "HABITATION 1 true");

        assertThat(reprise.reprendre()).isZero();
    }

    @Test
    void leClientTelechargeSesConditionsParticulieresAuto() throws Exception {
        byte[] pdf = telecharger(CLIENT_AUTO, document("AE-RECETTE-A03", "EN_VIGUEUR"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(texte(pdf)).contains("Conditions particulières - Assurance auto", "AE-RECETTE-A03",
                "Madame Camille Durand", "Renault Clio V TCe 90", "AB-003-CD", "Formule Tous risques",
                "IBAN •••• 7788", "réf. CG-AUTO, version 1", "Assur'Easy SA (assureur fictif)",
                "version 1 · généré le");
    }

    @Test
    void lesConditionsParticulieresHabitationEtGeneralesSeTelechargent() throws Exception {
        byte[] habitation = telecharger(CLIENT_1, document("AE-RECETTE-1", "EN_VIGUEUR"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        byte[] generales = telecharger(CLIENT_1, conditionsGenerales("HABITATION"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        assertThat(texte(habitation)).contains("Conditions particulières - Assurance habitation", "AE-RECETTE-1",
                "version 2", "réf. CG-HABITATION, version 1");
        assertThat(texte(generales)).contains("Conditions générales - Assurance habitation",
                GabaritPdf.MENTION_PROVISOIRE);
    }

    @Test
    void unAutreClientEstRefuseEtLeRefusEstTrace() throws Exception {
        long documentAuto = document("AE-RECETTE-A03", "EN_VIGUEUR");
        int refusAvant = jdbc.queryForObject("SELECT COUNT(*) FROM acces_refuse", Integer.class);

        telecharger(CLIENT_1, documentAuto).andExpect(status().isForbidden());
        telecharger(GESTIONNAIRE, documentAuto).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM acces_refuse", Integer.class)).isEqualTo(refusAvant + 1);
        assertThat(jdbc.queryForObject("SELECT motif FROM acces_refuse ORDER BY id DESC LIMIT 1", String.class))
                .isEqualTo("PAS_PROPRIETAIRE");
    }

    @Test
    void laRegenerationCreeLaVersionSuivanteEtRemplaceLaPrecedente() {
        long contrat = jdbc.queryForObject("SELECT id FROM contrat WHERE numero = 'AE-RECETTE-A05'", Long.class);
        DemandeDocument demande = DemandeDocument.duContrat(TypeDocument.CONDITIONS_PARTICULIERES, contrat,
                "Régénération (test T-03)");

        Document v2 = service.generer(demande);
        Document v3 = service.generer(demande);

        assertThat(v2.getVersion()).isEqualTo(2);
        assertThat(v3.getVersion()).isEqualTo(3);
        assertThat(jdbc.queryForList("""
                SELECT version || ' ' || statut || ' ' || stocke FROM document
                WHERE contrat_id = ? AND type = 'CONDITIONS_PARTICULIERES' ORDER BY version""", String.class,
                contrat)).containsExactly("1 REMPLACE true", "2 REMPLACE true", "3 EN_VIGUEUR true");
    }

    @Test
    void unEchecDeStockageNeBloquePasEtLeDocumentAttendSaReprise() throws Exception {
        long contrat = jdbc.queryForObject("SELECT id FROM contrat WHERE numero = 'AE-RECETTE-A06'", Long.class);

        // Aucun modèle de facture dans T-03 (US-22) : le PDF ne peut pas être produit.
        Document facture = service.generer(new DemandeDocument(TypeDocument.FACTURE, contrat, "FAC-2026-000901",
                "Test T-03"));

        assertThat(facture.getId()).isNotNull();
        assertThat(facture.getStocke()).isFalse();
        telecharger(CLIENT_AUTO, facture.getId()).andExpect(status().isConflict());
    }

    @Test
    void unPdfModifieNEstJamaisEnvoye() throws Exception {
        long contrat = jdbc.queryForObject("SELECT id FROM contrat WHERE numero = 'AE-RECETTE-A07'", Long.class);
        Document document = service.generer(DemandeDocument.duContrat(TypeDocument.CONDITIONS_PARTICULIERES, contrat,
                "Test d'intégrité"));
        jdbc.update("UPDATE document_contenu SET contenu = contenu || '\\x00'::BYTEA, taille_octets = taille_octets + 1"
                + " WHERE document_id = ?", document.getId());

        telecharger(CLIENT_AUTO, document.getId()).andExpect(status().isInternalServerError());
    }

    @Test
    void leTelechargementDUnDocumentInconnuRepond404() throws Exception {
        telecharger(CLIENT_AUTO, 999_999L).andExpect(status().isNotFound());
        assertThat(List.of(TypeDocument.values())).hasSize(12);
    }
}
