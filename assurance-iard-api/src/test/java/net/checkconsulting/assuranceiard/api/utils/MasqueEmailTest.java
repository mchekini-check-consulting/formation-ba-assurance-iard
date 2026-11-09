package net.checkconsulting.assuranceiard.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MasqueEmailTest {

    @ParameterizedTest
    @CsvSource({
        "marie.dupont@exemple.fr, m***@exemple.fr",
        "m@exemple.fr, m***@exemple.fr",
        "sansarobase, ***",
        "@exemple.fr, ***",
    })
    void masqueLaPartieAvantLArobase(String email, String attendu) {
        assertThat(MasqueEmail.masquer(email)).isEqualTo(attendu);
    }
}
