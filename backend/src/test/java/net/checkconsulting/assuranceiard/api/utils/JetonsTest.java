package net.checkconsulting.assuranceiard.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JetonsTest {

    @Test
    void genereDesJetonsDifferents() {
        assertThat(Jetons.nouveauJeton()).hasSizeGreaterThanOrEqualTo(43).isNotEqualTo(Jetons.nouveauJeton());
    }

    @Test
    void genereUnCodeASixChiffres() {
        assertThat(Jetons.nouveauCode()).matches("\\d{6}");
    }

    @Test
    void lEmpreinteEstStableEtTientDansLaColonne() {
        assertThat(Jetons.empreinte("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
                .hasSize(64);
    }
}
