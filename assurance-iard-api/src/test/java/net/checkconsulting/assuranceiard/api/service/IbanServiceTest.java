package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Uniquement des IBAN de test, publiés comme exemples. */
class IbanServiceTest {

    private final IbanService service = new IbanService();

    @ParameterizedTest
    @ValueSource(strings = {"FR7630006000011234567890189", "DE89370400440532013000", "BE71096123456769"})
    void accepteUnIbanValide(String iban) {
        assertThat(service.valide(iban)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "FR7630006000011234567890188",
        "FR7730006000011234567890189",
        "DE89370400440532013001"
    })
    void refuseUneCleDeControleFausse(String iban) {
        assertThat(service.valide(iban)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "FR76",
        "7630006000011234567890189FR",
        "FR763000600001123456789018",
        "FR76300060000112345678901890",
        "FR76-30006000011234567890189",
        "FRAB30006000011234567890189"
    })
    void refuseUnFormatInvalide(String iban) {
        assertThat(service.valide(iban)).isFalse();
    }

    @Test
    void refuseUnIbanFrancaisDeMauvaiseLongueurMemeAvecUneCleJuste() {
        assertThat(service.valide("FR1420041010050500013M026")).isFalse();
    }

    @Test
    void acceptePlusieursLettresDansLeCompte() {
        assertThat(service.valide("FR1420041010050500013M02606")).isTrue();
    }

    @Test
    void normaliseLaSaisieDuClient() {
        assertThat(service.normaliser(" fr76 3000 6000\t0112 3456 7890 189 ")).isEqualTo("FR7630006000011234567890189");
    }

    @Test
    void neGardeQueLes4DerniersCaracteres() {
        assertThat(service.fin("FR7630006000011234567890189")).isEqualTo("0189");
    }
}
