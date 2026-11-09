package net.checkconsulting.assuranceiard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.model.Justificatif;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.repository.DossierSouscriptionRepository;
import net.checkconsulting.assuranceiard.api.repository.JustificatifRepository;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/** US-13 sur une vraie base : migration V5, identité dans le compte, dossier, justificatifs et remplacement. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "assurance.justificatifs.dossier=" + InformationsIT.DOSSIER)
@AutoConfigureMockMvc
class InformationsIT {

    static final String DOSSIER = "target/justificatifs-it";

    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompteRepository comptes;

    @Autowired
    private RecommandationDevisRepository recommandations;

    @Autowired
    private DossierSouscriptionRepository dossiers;

    @Autowired
    private JustificatifRepository justificatifs;

    @Autowired
    private SessionService sessionService;

    private Compte compte;
    private String jeton;
    private UUID reference;

    @BeforeEach
    void preparer() {
        compte = new Compte();
        compte.setEmail(UUID.randomUUID() + "@exemple.fr");
        compte.setMotDePasseHash("hash");
        compte = comptes.save(compte);
        jeton = "Bearer " + sessionService.ouvrir(compte).jeton();

        RecommandationDevis recommandation = new RecommandationDevis();
        recommandation.setReference(UUID.randomUUID());
        recommandation.setRemboursement("neuf");
        recommandation.setPriorite("prix");
        recommandation.setFormule("confort");
        recommandation.setRaisons(List.of("raison"));
        recommandation.setOptions(List.of());
        recommandation.setDateCreation(Instant.now());
        reference = recommandations.save(recommandation).getReference();
    }

    private String informations(String nom) {
        return """
                {"referenceDevis":"%s","civilite":"madame","nom":"%s","prenom":"Marie",
                "dateNaissance":"1990-01-31","telephone":"0612345678","typePieceIdentite":"passeport",
                "typeJustificatifDomicile":"quittance-loyer","colocataires":["Paul Martin","Léa Petit"]}
                """.formatted(reference, nom);
    }

    private void deposer(String nom, byte[] contenuDomicile) throws Exception {
        mockMvc.perform(multipart("/devis/informations")
                        .file(new MockMultipartFile(
                                "informations", "", MediaType.APPLICATION_JSON_VALUE,
                                informations(nom).getBytes(StandardCharsets.UTF_8)))
                        .file(new MockMultipartFile("pieceIdentite", "carte.pdf", null, PDF))
                        .file(new MockMultipartFile("justificatifDomicile", "quittance.png", null, contenuDomicile))
                        .header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isNoContent());
    }

    private List<Justificatif> depotsDuDossier() {
        DossierSouscription dossier = dossiers.findByCompteAndReferenceDevis(compte, reference).orElseThrow();
        return justificatifs.findByDossier(dossier);
    }

    private long nombreDeFichiers(List<Justificatif> depots) throws IOException {
        try (Stream<Path> fichiers = Files.list(Path.of(DOSSIER))) {
            List<String> presents = fichiers.map(chemin -> chemin.getFileName().toString()).toList();
            return depots.stream().map(Justificatif::getCleStockage).filter(presents::contains).count();
        }
    }

    @Test
    void lesInformationsSontEnregistreesPuisRelues() throws Exception {
        mockMvc.perform(get("/devis/informations").header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").doesNotExist());

        deposer("Durand", PNG);

        mockMvc.perform(get("/devis/informations").header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.civilite").value("madame"))
                .andExpect(jsonPath("$.nom").value("Durand"))
                .andExpect(jsonPath("$.dateNaissance").value("1990-01-31"));

        Compte relu = comptes.findById(compte.getId()).orElseThrow();
        assertThat(relu.getDateNaissance()).isEqualTo(LocalDate.of(1990, 1, 31));
        DossierSouscription dossier = dossiers.findByCompteAndReferenceDevis(compte, reference).orElseThrow();
        assertThat(dossier.getColocataires()).containsExactly("Paul Martin", "Léa Petit");
        assertThat(depotsDuDossier())
                .extracting(Justificatif::getCategorie, Justificatif::getTypeDocument, Justificatif::getTypeFichier)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("identite", "passeport", "pdf"),
                        org.assertj.core.groups.Tuple.tuple("domicile", "quittance-loyer", "png"));
        assertThat(nombreDeFichiers(depotsDuDossier())).isEqualTo(2);
    }

    @Test
    void unNouveauDepotRemplaceLePrecedentSansRienLaisserTrainer() throws Exception {
        deposer("Durand", PNG);
        List<String> premieresCles = depotsDuDossier().stream().map(Justificatif::getCleStockage).toList();

        deposer("Dupont", PNG);

        assertThat(depotsDuDossier()).hasSize(2);
        assertThat(nombreDeFichiers(depotsDuDossier())).isEqualTo(2);
        premieresCles.forEach(cle -> assertThat(Path.of(DOSSIER, cle)).doesNotExist());
        assertThat(comptes.findById(compte.getId()).orElseThrow().getNom()).isEqualTo("Dupont");
    }

    @Test
    void unFauxFichierNeLaisseRienEnBase() throws Exception {
        mockMvc.perform(multipart("/devis/informations")
                        .file(new MockMultipartFile(
                                "informations", "", MediaType.APPLICATION_JSON_VALUE,
                                informations("Durand").getBytes(StandardCharsets.UTF_8)))
                        .file(new MockMultipartFile("pieceIdentite", "carte.pdf", null, new byte[] {1, 2, 3, 4, 5}))
                        .file(new MockMultipartFile("justificatifDomicile", "quittance.png", null, PNG))
                        .header(HttpHeaders.AUTHORIZATION, jeton))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champ").value("pieceIdentite"));

        assertThat(dossiers.findByCompteAndReferenceDevis(compte, reference)).isEmpty();
        assertThat(comptes.findById(compte.getId()).orElseThrow().getNom()).isNull();
    }

    @Test
    void sansSessionRienNEstLuNiEnregistre() throws Exception {
        mockMvc.perform(get("/devis/informations")).andExpect(status().isUnauthorized());
    }
}
