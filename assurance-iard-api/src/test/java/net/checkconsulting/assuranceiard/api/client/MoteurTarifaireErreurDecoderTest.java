package net.checkconsulting.assuranceiard.api.client;

import static org.assertj.core.api.Assertions.assertThat;

import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import net.checkconsulting.assuranceiard.api.dto.ErreurChampMoteur;
import net.checkconsulting.assuranceiard.api.exception.MoteurDonneesInvalidesException;
import net.checkconsulting.assuranceiard.api.exception.MoteurIndisponibleException;
import net.checkconsulting.assuranceiard.api.exception.MoteurTarifExpireException;
import net.checkconsulting.assuranceiard.api.exception.MoteurTarifInconnuException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class MoteurTarifaireErreurDecoderTest {

    private final MoteurTarifaireErreurDecoder decoder = new MoteurTarifaireErreurDecoder(JsonMapper.builder().build());

    private Exception decoder(int statut, String corps) {
        Request requete = Request.create(Request.HttpMethod.POST, "/", Map.of(), null, StandardCharsets.UTF_8, null);
        Response.Builder reponse = Response.builder().status(statut).request(requete).headers(Map.of());
        if (corps != null) {
            reponse.body(corps, StandardCharsets.UTF_8);
        }
        return decoder.decode("calculer", reponse.build());
    }

    @Test
    void uneErreur400RepriseAvecSesChamps() {
        Exception erreur = decoder(400, "{\"detail\":\"x\",\"erreurs\":[{\"champ\":\"etage\",\"message\":\"Obligatoire\"}]}");

        assertThat(erreur).isInstanceOfSatisfying(MoteurDonneesInvalidesException.class, invalide ->
                assertThat(invalide.getErreurs()).isEqualTo(List.of(new ErreurChampMoteur("etage", "Obligatoire"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pas du json", "{\"detail\":\"sans liste\"}"})
    void uneErreur400AvecUnCorpsInattenduResteUneErreur400(String corps) {
        assertThat(decoder(400, corps)).isInstanceOfSatisfying(MoteurDonneesInvalidesException.class,
                invalide -> assertThat(invalide.getErreurs()).isEmpty());
    }

    @Test
    void uneErreur400SansCorpsResteUneErreur400() {
        assertThat(decoder(400, null)).isInstanceOf(MoteurDonneesInvalidesException.class);
    }

    @Test
    void uneErreur404DonneTarifInconnu() {
        assertThat(decoder(404, null)).isInstanceOf(MoteurTarifInconnuException.class);
    }

    @Test
    void uneErreur410DonneTarifExpire() {
        assertThat(decoder(410, null)).isInstanceOf(MoteurTarifExpireException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 503})
    void uneErreurDuMoteurDonneIndisponible(int statut) {
        assertThat(decoder(statut, null)).isInstanceOf(MoteurIndisponibleException.class);
    }
}
