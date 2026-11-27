package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import net.checkconsulting.assuranceiard.api.dto.IbanVerificationResponse;
import org.junit.jupiter.api.Test;

class IbanBouchonTest {

    private final IbanBouchon bouchon = new IbanBouchon();

    @Test
    void lIbanBnpEstReconnuAvecSonBicExact() {
        IbanVerificationResponse rep = bouchon.verifier("FR7630006000011234567890189");
        assertThat(rep.valide()).isTrue();
        assertThat(rep.bic()).isEqualTo("BNPAFRPPXXX");
        assertThat(rep.banque()).isEqualTo("BNP Paribas");
    }

    @Test
    void lIbanBresillienEstRefuseCarHorsSEPA() {
        IbanVerificationResponse rep = bouchon.verifier("BR1800360305000010009795493C1");
        assertThat(rep.valide()).isFalse();
        assertThat(rep.zoneSepa()).isFalse();
        assertThat(rep.messageErreur()).contains("SEPA");
    }

    @Test
    void unAutreIbanValideRetourneLesBancairesDTest() {
        IbanVerificationResponse rep = bouchon.verifier("DE89370400440532013000");
        assertThat(rep.valide()).isTrue();
        assertThat(rep.bic()).isEqualTo("TESTBICX");
        assertThat(rep.banque()).isEqualTo("Banque de test");
    }

    @Test
    void lIbanMasqueNeContientQueLes4DerniersCaracteres() {
        IbanVerificationResponse rep = bouchon.verifier("FR7630006000011234567890189");
        assertThat(rep.ibanMasque()).isEqualTo("•••• 0189");
        assertThat(rep.ibanMasque()).doesNotContain("FR76");
        assertThat(rep.ibanMasque()).doesNotContain("FR7630006000011234567890189".substring(0, 20));
    }
}
