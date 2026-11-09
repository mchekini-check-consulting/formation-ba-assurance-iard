package net.checkconsulting.moteurtarifaire.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.checkconsulting.moteurtarifaire.api.model.CalculTarif;
import net.checkconsulting.moteurtarifaire.api.repository.CalculTarifRepository;
import net.checkconsulting.moteurtarifaire.api.repository.ParametreTarifaireRepository;
import net.checkconsulting.moteurtarifaire.api.repository.VersionGrilleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TarifHabitationIT {

    private static final String ADRESSE = "/v1/tarifs/habitation";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private CalculTarifRepository repository;

    @Autowired
    private Clock horloge;

    @Autowired
    private VersionGrilleRepository versions;

    @Autowired
    private ParametreTarifaireRepository parametres;

    @BeforeEach
    void installerLaGrille() {
        JeuxDeDonnees.installerGrilleDeReference(versions, parametres);
    }

    private ResultActions envoyer(Map<String, Object> corps) throws Exception {
        return mockMvc.perform(post(ADRESSE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(corps)));
    }

    private Map<String, Object> corpsAvec(String champ, Object valeur) {
        Map<String, Object> corps = JeuxDeDonnees.corpsCa1();
        corps.put(champ, valeur);
        return corps;
    }

    private String champDeLaReponse(ResultActions reponse, String champ) throws Exception {
        return jsonMapper.readTree(reponse.andReturn().getResponse().getContentAsString()).get(champ).asString();
    }

    @Test
    void ca1CalculNominal() throws Exception {
        envoyer(JeuxDeDonnees.corpsCa1())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifable").value(true))
                .andExpect(jsonPath("$.versionGrille").value(JeuxDeDonnees.GRILLE.version()))
                .andExpect(jsonPath("$.prix[0].formule").value("ESSENTIELLE"))
                .andExpect(jsonPath("$.prix[0].mensuel").value(8.69))
                .andExpect(jsonPath("$.prix[1].formule").value("CONFORT"))
                .andExpect(jsonPath("$.prix[1].mensuel").value(11.99))
                .andExpect(jsonPath("$.prix[2].formule").value("SERENITE"))
                .andExpect(jsonPath("$.prix[2].mensuel").value(17.49))
                .andExpect(jsonPath("$.dateValidite")
                        .value(LocalDate.now(horloge).plusDays(JeuxDeDonnees.GRILLE.dureeValiditeJours()).toString()));
    }

    @Test
    void ca2FormuleOptionEtFranchise() throws Exception {
        Map<String, Object> corps = JeuxDeDonnees.corpsCa1();
        corps.put("formule", "CONFORT");
        corps.put("options", List.of("OBJETS_VALEUR"));
        corps.put("franchise", 300);

        envoyer(corps)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail.prixFormule").value(11.99))
                .andExpect(jsonPath("$.detail.options[0].option").value("OBJETS_VALEUR"))
                .andExpect(jsonPath("$.detail.totalMensuel").value(14.49))
                .andExpect(jsonPath("$.detail.totalAnnuel").value(173.88));
    }

    @Test
    void ca3ProprietaireNonOccupant() throws Exception {
        verifierRefus(envoyer(corpsAvec("statut", "PROPRIETAIRE_NON_OCCUPANT")));
    }

    @Test
    void ca3ResiliationPourNonPaiement() throws Exception {
        verifierRefus(envoyer(corpsAvec("resiliationAnterieure", "NON_PAIEMENT")));
    }

    private void verifierRefus(ResultActions reponse) throws Exception {
        reponse.andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifable").value(false))
                .andExpect(jsonPath("$.motif").isNotEmpty())
                .andExpect(jsonPath("$.orientation").value("CONSEILLER"))
                .andExpect(jsonPath("$.prix").doesNotExist())
                .andExpect(jsonPath("$.detail").doesNotExist());
    }

    @Test
    void ca4ChampObligatoireAbsent() throws Exception {
        Map<String, Object> corps = JeuxDeDonnees.corpsCa1();
        corps.remove("codePostal");

        envoyer(corps)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erreurs[0].champ").value("codePostal"))
                .andExpect(jsonPath("$.erreurs[0].message").value("Ce champ est obligatoire."));
    }

    @Test
    void ca5AppartementSansEtage() throws Exception {
        Map<String, Object> corps = JeuxDeDonnees.corpsCa1();
        corps.remove("etage");

        envoyer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath("$.erreurs[0].champ").value("etage"));
    }

    @Test
    void ca6ContenuDePlusDe35000Euros() throws Exception {
        Map<String, Object> besoins = JeuxDeDonnees.corpsBesoins();
        besoins.put("valeurContenu", 35_001);
        besoins.put("assistance", true);

        envoyer(corpsAvec("besoins", besoins))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formuleConseillee").value("SERENITE"))
                .andExpect(jsonPath("$.raisons[0]").isNotEmpty())
                .andExpect(jsonPath("$.optionsConseillees[0]").value("ASSISTANCE"));
    }

    @Test
    void essentielleEtConfortSontBienConseillees() throws Exception {
        Map<String, Object> besoins = JeuxDeDonnees.corpsBesoins();
        envoyer(corpsAvec("besoins", besoins)).andExpect(jsonPath("$.formuleConseillee").value("CONFORT"));

        besoins.put("valeurContenu", 1_000);
        besoins.put("priorite", "PRIX");
        envoyer(corpsAvec("besoins", besoins)).andExpect(jsonPath("$.formuleConseillee").value("ESSENTIELLE"));
    }

    @Test
    void uneFranchiseAbsenteDeLaGrilleEstRefusee() throws Exception {
        envoyer(corpsAvec("franchise", 151))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs[0].champ").value("franchise"));
    }

    @Test
    void ca7LeCalculEstEnregistre() throws Exception {
        UUID idTarif = UUID.fromString(champDeLaReponse(envoyer(JeuxDeDonnees.corpsCa1()), "idTarif"));

        CalculTarif trace = repository.findByIdTarif(idTarif).orElseThrow();
        assertThat(trace.getVersionGrille()).isEqualTo(JeuxDeDonnees.GRILLE.version());
        assertThat(trace.isTarifable()).isTrue();
        assertThat(trace.getDonneesRecues()).contains(JeuxDeDonnees.corpsCa1().get("codePostal").toString());
        assertThat(trace.getResultat()).contains(idTarif.toString());
        assertThat(trace.getDateCalcul()).isNotNull();
        assertThat(trace.getDateValidite()).isAfter(LocalDate.now(horloge));
    }

    @Test
    void ca7UnRefusEstAussiEnregistre() throws Exception {
        UUID idTarif = UUID.fromString(champDeLaReponse(envoyer(corpsAvec("statut", "PROPRIETAIRE_NON_OCCUPANT")), "idTarif"));

        CalculTarif trace = repository.findByIdTarif(idTarif).orElseThrow();
        assertThat(trace.isTarifable()).isFalse();
        assertThat(trace.getDateValidite()).isNull();
    }

    @Test
    void ca7UneRepriseRendExactementLeCalculDOrigine() throws Exception {
        ResultActions calcul = envoyer(JeuxDeDonnees.corpsCa1());
        String idTarif = champDeLaReponse(calcul, "idTarif");
        var attendu = jsonMapper.readTree(calcul.andReturn().getResponse().getContentAsString());

        String reprise = mockMvc.perform(get(ADRESSE + "/" + idTarif))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(jsonMapper.readTree(reprise)).isEqualTo(attendu);
    }

    @Test
    void ca7UneRepriseGardeLePrixDOrigineMemeSiLaGrilleAChange() throws Exception {
        UUID idTarif = UUID.randomUUID();
        double prixDOrigine = 1.23;
        enregistrerTarif(idTarif, LocalDate.now(horloge), prixDOrigine);

        mockMvc.perform(get(ADRESSE + "/" + idTarif))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prix[0].mensuel").value(prixDOrigine));
    }

    @Test
    void unTarifExpireNEstPlusRepris() throws Exception {
        UUID idTarif = UUID.randomUUID();
        enregistrerTarif(idTarif, LocalDate.now(horloge).minusDays(1), 1.23);

        mockMvc.perform(get(ADRESSE + "/" + idTarif))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.detail").value(containsString("expiré")));
    }

    private void enregistrerTarif(UUID idTarif, LocalDate dateValidite, double prix) {
        CalculTarif calcul = new CalculTarif();
        calcul.setIdTarif(idTarif);
        calcul.setVersionGrille("ancienne-version");
        calcul.setTarifable(true);
        calcul.setDonneesRecues("{}");
        calcul.setResultat(jsonMapper.writeValueAsString(Map.of(
                "idTarif", idTarif,
                "tarifable", true,
                "dateValidite", dateValidite.toString(),
                "prix", List.of(Map.of("formule", "ESSENTIELLE", "mensuel", prix)))));
        calcul.setDateCalcul(Instant.EPOCH);
        calcul.setDateValidite(dateValidite);
        repository.save(calcul);
    }

    @Test
    void unTarifInconnuDonne404() throws Exception {
        mockMvc.perform(get(ADRESSE + "/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void laDocumentationOpenApiDecritLAdresse() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['" + ADRESSE + "'].post").exists())
                .andExpect(jsonPath("$.components.schemas.TarifHabitationRequest.properties.statut.enum",
                        hasItem("LOCATAIRE")))
                .andExpect(jsonPath("$.paths.keys()", not(hasItem("/error"))));
    }

    @Test
    void ca8UnChampNominatifEstIgnoreEtNArriveJamaisEnBase() throws Exception {
        String nom = "Dupont";

        UUID idTarif = UUID.fromString(champDeLaReponse(envoyer(corpsAvec("nom", nom)).andExpect(status().isOk()), "idTarif"));

        assertThat(repository.findByIdTarif(idTarif).orElseThrow().getDonneesRecues()).doesNotContain(nom);
    }

    @Test
    void unCodePostalHorsDepartementDonne400() throws Exception {
        envoyer(corpsAvec("codePostal", "00000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs[0].champ").value("codePostal"));
    }
}
