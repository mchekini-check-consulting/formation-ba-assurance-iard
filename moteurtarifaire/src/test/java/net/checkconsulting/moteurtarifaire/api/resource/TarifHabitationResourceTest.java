package net.checkconsulting.moteurtarifaire.api.resource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees;
import net.checkconsulting.moteurtarifaire.api.dto.TarifHabitationResponse;
import net.checkconsulting.moteurtarifaire.api.exception.DonneeInvalideException;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleIndisponibleException;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleInvalideException;
import net.checkconsulting.moteurtarifaire.api.exception.TarifExpireException;
import net.checkconsulting.moteurtarifaire.api.exception.TarifInconnuException;
import net.checkconsulting.moteurtarifaire.api.service.TarifService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(TarifHabitationResource.class)
class TarifHabitationResourceTest {

    private static final String ADRESSE = "/v1/tarifs/habitation";
    private static final String CHAMP_OBLIGATOIRE = "Ce champ est obligatoire.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private TarifService tarifService;

    private ResultActions envoyer(Map<String, Object> corps) throws Exception {
        return mockMvc.perform(post(ADRESSE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(corps)));
    }

    private void verifierErreurDeChamp(ResultActions reponse, String champ, String message) throws Exception {
        verify(tarifService, never()).calculer(any());
        reponse.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs", hasSize(1)))
                .andExpect(jsonPath("$.erreurs[0].champ").value(champ))
                .andExpect(jsonPath("$.erreurs[0].message").value(containsString(message)));
    }

    @Test
    void unAppelValideRenvoieLeResultatDuService() throws Exception {
        UUID idTarif = UUID.randomUUID();
        when(tarifService.calculer(any()))
                .thenReturn(TarifHabitationResponse.builder().idTarif(idTarif).tarifable(true).build());

        envoyer(JeuxDeDonnees.corpsCa1())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTarif").value(idTarif.toString()))
                .andExpect(jsonPath("$.tarifable").value(true))
                .andExpect(jsonPath("$.motif").doesNotExist())
                .andExpect(jsonPath("$.prix").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "statut", "residence", "typeLogement", "codePostal", "surface", "pieces", "etage", "sinistres3ans", "resiliationAnterieure"
    })
    void ca4UnChampObligatoireAbsentEstSignale(String champ) throws Exception {
        Map<String, Object> corps = JeuxDeDonnees.corpsCa1();
        corps.remove(champ);

        envoyer(corps)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs[?(@.champ == '" + champ + "')].message").value(
                        hasItem(containsString("obligatoire"))));
        verify(tarifService, never()).calculer(any());
    }

    @Test
    void ca4TousLesChampsEnErreurSontListes() throws Exception {
        envoyer(Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Demande invalide"))
                .andExpect(jsonPath("$.erreurs", hasSize(8)))
                .andExpect(jsonPath("$.erreurs[0].message").value(CHAMP_OBLIGATOIRE));
    }

    @ParameterizedTest
    @CsvSource({
        "surface, 8.99, supérieure ou égale",
        "surface, 1000.01, inférieure ou égale",
        "pieces, 0, supérieure ou égale",
        "pieces, 6, inférieure ou égale",
        "sinistres3ans, -1, supérieure ou égale",
        "sinistres3ans, 4, inférieure ou égale"
    })
    void ca4UneValeurHorsLimitesDonneLeMotifPrecis(String champ, BigDecimal valeur, String motif) throws Exception {
        Map<String, Object> corps = JeuxDeDonnees.corpsCa1();
        corps.put(champ, valeur);

        verifierErreurDeChamp(envoyer(corps), champ, motif);
    }

    @ParameterizedTest
    @ValueSource(strings = {"6900", "690031", "6900A", ""})
    void ca4UnCodePostalMalFormeEstRefuse(String codePostal) throws Exception {
        Map<String, Object> corps = JeuxDeDonnees.corpsCa1();
        corps.put("codePostal", codePostal);

        verifierErreurDeChamp(envoyer(corps), "codePostal", "5 chiffres");
    }

    @Test
    void ca4UneValeurDEnumerationInconnueListeLesValeursPossibles() throws Exception {
        Map<String, Object> corps = JeuxDeDonnees.corpsCa1();
        corps.put("statut", "VOISIN");

        verifierErreurDeChamp(envoyer(corps), "statut", "LOCATAIRE");
    }

    @Test
    void ca5UnAppartementSansEtageEstRefuse() throws Exception {
        Map<String, Object> corps = JeuxDeDonnees.corpsCa1();
        corps.remove("etage");

        verifierErreurDeChamp(envoyer(corps), "etage", "appartement");
    }

    @Test
    void ca5UneMaisonSansDependancesEstRefusee() throws Exception {
        Map<String, Object> corps = JeuxDeDonnees.corpsCa1();
        corps.put("typeLogement", "MAISON");
        corps.remove("etage");

        verifierErreurDeChamp(envoyer(corps), "dependances", "maison");
    }

    @Test
    void uneMaisonAvecDependancesEstAcceptee() throws Exception {
        Map<String, Object> corps = JeuxDeDonnees.corpsCa1();
        corps.put("typeLogement", "MAISON");
        corps.remove("etage");
        corps.put("dependances", "JARDIN");
        when(tarifService.calculer(any())).thenReturn(TarifHabitationResponse.builder().build());

        envoyer(corps).andExpect(status().isOk());
    }

    @Test
    void desBesoinsIncompletsSontRefusesSansValeurParDefaut() throws Exception {
        Map<String, Object> besoins = JeuxDeDonnees.corpsBesoins();
        besoins.remove("priorite");
        Map<String, Object> corps = JeuxDeDonnees.corpsCa1();
        corps.put("besoins", besoins);

        verifierErreurDeChamp(envoyer(corps), "besoins.priorite", "obligatoire");
    }

    @Test
    void uneErreurDansUnObjetImbriqueDonneLeCheminComplet() throws Exception {
        Map<String, Object> besoins = JeuxDeDonnees.corpsBesoins();
        besoins.put("priorite", "INCONNUE");
        Map<String, Object> corps = JeuxDeDonnees.corpsCa1();
        corps.put("besoins", besoins);

        verifierErreurDeChamp(envoyer(corps), "besoins.priorite", "PRIX");
    }

    @Test
    void uneErreurDansUneListeDonneLaPosition() throws Exception {
        Map<String, Object> corps = JeuxDeDonnees.corpsCa1();
        corps.put("options", List.of("INCONNUE"));

        verifierErreurDeChamp(envoyer(corps), "options[0]", "OBJETS_VALEUR");
    }

    @Test
    void unJsonIllisibleEstRefuse() throws Exception {
        mockMvc.perform(post(ADRESSE).contentType(MediaType.APPLICATION_JSON).content("{pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("JSON")));
    }

    @Test
    void uneValeurRefuseeParLaGrilleEstSignaleeSurSonChamp() throws Exception {
        when(tarifService.calculer(any())).thenThrow(new DonneeInvalideException("franchise", "Franchise non proposée."));

        envoyer(JeuxDeDonnees.corpsCa1())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs[0].champ").value("franchise"));
    }

    @Test
    void sansGrilleLeServiceEstIndisponible() throws Exception {
        when(tarifService.calculer(any())).thenThrow(new GrilleIndisponibleException(LocalDate.EPOCH));

        envoyer(JeuxDeDonnees.corpsCa1())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(containsString("grille")));
    }

    @Test
    void uneErreurInattendueNeDivulgueAucunDetailTechnique() throws Exception {
        String detailTechnique = "détail technique secret";
        when(tarifService.calculer(any())).thenThrow(new IllegalStateException(detailTechnique));

        envoyer(JeuxDeDonnees.corpsCa1())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(not(containsString(detailTechnique))));
    }

    @Test
    void uneRepriseRenvoieLeTarifDOrigine() throws Exception {
        UUID idTarif = UUID.randomUUID();
        when(tarifService.reprendre(idTarif)).thenReturn(TarifHabitationResponse.builder().idTarif(idTarif).build());

        mockMvc.perform(get(ADRESSE + "/" + idTarif))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTarif").value(idTarif.toString()));
    }

    @Test
    void unTarifInconnuDonne404() throws Exception {
        UUID idTarif = UUID.randomUUID();
        when(tarifService.reprendre(idTarif)).thenThrow(new TarifInconnuException(idTarif));

        mockMvc.perform(get(ADRESSE + "/" + idTarif)).andExpect(status().isNotFound());
    }

    @Test
    void unTarifExpireDonne410AvecSaDate() throws Exception {
        UUID idTarif = UUID.randomUUID();
        when(tarifService.reprendre(idTarif)).thenThrow(new TarifExpireException(LocalDate.EPOCH));

        mockMvc.perform(get(ADRESSE + "/" + idTarif))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.dateValidite").value(LocalDate.EPOCH.toString()));
    }

    @Test
    void unIdentifiantMalFormeDonne400EnFrancais() throws Exception {
        mockMvc.perform(get(ADRESSE + "/pas-un-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("paramètre")));
    }

    @Test
    void uneMethodeNonAutoriseeRepondEnFrancais() throws Exception {
        mockMvc.perform(get(ADRESSE))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.title").value("Méthode non autorisée"))
                .andExpect(jsonPath("$.detail").value(containsString("n'est pas autorisée")));
    }

    @Test
    void uneGrilleInvalideDonne503SansDetailInterne() throws Exception {
        String detailInterne = "version 2026.1 incomplète";
        when(tarifService.calculer(any())).thenThrow(new GrilleInvalideException(detailInterne));

        envoyer(JeuxDeDonnees.corpsCa1())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(not(containsString(detailInterne))));
    }
}
