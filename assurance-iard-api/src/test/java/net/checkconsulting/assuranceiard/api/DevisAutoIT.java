package net.checkconsulting.assuranceiard.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import net.checkconsulting.assuranceiard.api.client.MoteurTarifaireClient;
import net.checkconsulting.assuranceiard.api.dto.CriteresEligibiliteResponse;
import net.checkconsulting.assuranceiard.api.dto.TarifAutoResponse;
import net.checkconsulting.assuranceiard.api.model.RefPlaqueTest;
import net.checkconsulting.assuranceiard.api.model.RefVehicule;
import net.checkconsulting.assuranceiard.api.repository.RefPlaqueTestRepository;
import net.checkconsulting.assuranceiard.api.repository.RefVehiculeRepository;
import net.checkconsulting.assuranceiard.api.service.EligibiliteAutoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-27 : tests d'intégration du parcours de tarification auto (recherche SIV, référentiels).
 * EligibiliteAutoService est mocké pour ne pas appeler le moteurtarifaire durant les tests.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DevisAutoIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RefVehiculeRepository vehicules;

    @Autowired
    private RefPlaqueTestRepository plaques;

    @MockitoBean
    private EligibiliteAutoService eligibiliteAutoService;

    @MockitoBean
    private MoteurTarifaireClient moteurTarifaireClient;

    private Long idPeugeot;

    @BeforeEach
    void installerDonnees() {
        plaques.deleteAll();
        vehicules.deleteAll();
        RefVehicule peugeot = new RefVehicule();
        peugeot.setMarque("Peugeot");
        peugeot.setModele("308");
        peugeot.setEnergie("ESSENCE");
        peugeot.setVersion("1.2 PureTech 130");
        peugeot.setAnnee(2021);
        peugeot.setValeurNeuve(new BigDecimal("18500.00"));
        idPeugeot = vehicules.save(peugeot).getId();

        RefVehicule renault = new RefVehicule();
        renault.setMarque("Renault");
        renault.setModele("Clio");
        renault.setEnergie("ESSENCE");
        renault.setAnnee(2022);
        renault.setValeurNeuve(new BigDecimal("14200.00"));
        vehicules.save(renault);

        RefPlaqueTest trouve = new RefPlaqueTest();
        trouve.setImmatriculation("AB-123-CD");
        trouve.setRefVehiculeId(idPeugeot);
        trouve.setComportement("TROUVE");
        plaques.save(trouve);

        RefPlaqueTest inconnu = new RefPlaqueTest();
        inconnu.setImmatriculation("XX-000-XX");
        inconnu.setComportement("INCONNU");
        plaques.save(inconnu);

        RefPlaqueTest erreur = new RefPlaqueTest();
        erreur.setImmatriculation("EE-500-EE");
        erreur.setComportement("ERREUR");
        plaques.save(erreur);

        RefPlaqueTest delai = new RefPlaqueTest();
        delai.setImmatriculation("DD-408-DD");
        delai.setComportement("DELAI");
        plaques.save(delai);

        Mockito.when(eligibiliteAutoService.criteres())
                .thenReturn(new CriteresEligibiliteResponse(new BigDecimal("1.50"), "NON_PAIEMENT"));

        Mockito.when(moteurTarifaireClient.calculerAuto(Mockito.any()))
                .thenReturn(new TarifAutoResponse(
                        java.util.UUID.randomUUID(), "2026-1", true, "TIERS",
                        java.util.List.of(new TarifAutoResponse.PrixFormule("TIERS", new BigDecimal("25.00"), new BigDecimal("300.00"))),
                        null, null, null, "Peugeot", "308", new BigDecimal("18500.00")));
    }

    @Test
    void ca1RecherchePlaqueTrouveLeVehicule() throws Exception {
        mockMvc.perform(post("/v1/devis/auto/recherche-vehicule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"immatriculation\":\"AB-123-CD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("TROUVE"))
                .andExpect(jsonPath("$.marque").value("Peugeot"))
                .andExpect(jsonPath("$.modele").value("308"))
                .andExpect(jsonPath("$.refVehiculeId").value(idPeugeot));
    }

    @Test
    void ca1RecherchePlaqueSansNormalisationFonctionne() throws Exception {
        mockMvc.perform(post("/v1/devis/auto/recherche-vehicule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"immatriculation\":\"AB123CD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("TROUVE"))
                .andExpect(jsonPath("$.immatriculation").value("AB-123-CD"));
    }

    @Test
    void ca1RecherchePlaquePlaqueInconnueRenvoieInconnu() throws Exception {
        mockMvc.perform(post("/v1/devis/auto/recherche-vehicule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"immatriculation\":\"XX-000-XX\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("INCONNU"))
                .andExpect(jsonPath("$.marque").doesNotExist());
    }

    @Test
    void ca1RecherchePlaqueErreurSivRenvoieErreur() throws Exception {
        mockMvc.perform(post("/v1/devis/auto/recherche-vehicule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"immatriculation\":\"EE-500-EE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ERREUR"));
    }

    @Test
    void ca1RecherchePlaqueSivEnDelaiRenvoieDelai() throws Exception {
        mockMvc.perform(post("/v1/devis/auto/recherche-vehicule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"immatriculation\":\"DD-408-DD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("DELAI"));
    }

    @Test
    void ca1RecherchePlaqueVideDonne400() throws Exception {
        mockMvc.perform(post("/v1/devis/auto/recherche-vehicule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"immatriculation\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ca1LeReferentielVehiculesContientLesMarques() throws Exception {
        mockMvc.perform(get("/v1/devis/auto/referentiels/vehicules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].marque").value("Peugeot"))
                .andExpect(jsonPath("$[1].marque").value("Renault"));
    }

    @Test
    void ca1LesCriteresEligibiliteViennentDuService() throws Exception {
        mockMvc.perform(get("/v1/devis/auto/referentiels/criteres-eligibilite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bonusMalusMaxLigne").value(1.5))
                .andExpect(jsonPath("$.resiliationBloquante").value("NON_PAIEMENT"));
    }

    @Test
    void ca4TarifRetourneLeResultatDuMoteur() throws Exception {
        mockMvc.perform(post("/v1/devis/auto/tarif")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refVehiculeId":%d,"anneePermis":2010,"bonusMalus":0.90,
                                "kmAnnuels":11500,"usage":"PRIVE","stationnement":"GARAGE_FERME"}
                                """.formatted(idPeugeot)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifable").value(true))
                .andExpect(jsonPath("$.prix[0].mensuel").value(25.0));
    }

    @Test
    void ca4CreerDevisValideRenvoie201() throws Exception {
        mockMvc.perform(post("/v1/devis/auto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refVehiculeId":%d,"nonImmatricule":false,"titulaireCarteGrise":"SOUSCRIPTEUR",
                                "usage":"PRIVE","stationnement":"GARAGE_FERME","kmAnnuels":11500,"codePostal":"75000",
                                "conducteurPrincipal":"MOI_MEME","dateNaissance":"15/06/1990","datePermis":"20/03/2010",
                                "bonusMalus":0.90,"anneesAssurance":5,"conduiteAccompagnee":false,
                                "nbSinistresResponsables":0,"nbSinistresNonResponsables":0,"resiliationAssureur":"AUCUNE",
                                "suspensionAnnulation":false,"condamnationAlcool":false,"assureVehicule":"NON"}
                                """.formatted(idPeugeot)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.devisAutoId").value(notNullValue()))
                .andExpect(jsonPath("$.eligibleEnLigne").value(true));
    }

    @Test
    void ca4CreerDevisProfilBloqueRenvoie422() throws Exception {
        mockMvc.perform(post("/v1/devis/auto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refVehiculeId":%d,"nonImmatricule":false,"titulaireCarteGrise":"SOUSCRIPTEUR",
                                "usage":"PRIVE","stationnement":"GARAGE_FERME","kmAnnuels":11500,"codePostal":"75000",
                                "conducteurPrincipal":"MOI_MEME","dateNaissance":"15/06/1990","datePermis":"20/03/2010",
                                "bonusMalus":1.80,"anneesAssurance":5,"conduiteAccompagnee":false,
                                "nbSinistresResponsables":0,"nbSinistresNonResponsables":0,"resiliationAssureur":"AUCUNE",
                                "suspensionAnnulation":false,"condamnationAlcool":false,"assureVehicule":"NON"}
                                """.formatted(idPeugeot)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.motif").value("BONUS_MALUS"));
    }
}
