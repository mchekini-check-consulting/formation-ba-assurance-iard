package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import net.checkconsulting.assuranceiard.api.config.ConnexionProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class EnvoiLienBouchonTest {

    private static final String LIEN = "http://ecran/reinitialisation-mot-de-passe?jeton=secret";

    @Test
    void ecritLeLienAvecLAdresseMasqueeQuandLeBouchonEstActif(CapturedOutput sortie) {
        new EnvoiLienBouchon(new ConnexionProperties(true, "http://ecran")).envoyer("marie@exemple.fr", LIEN);

        assertThat(sortie.getOut()).contains(LIEN).contains("m***@exemple.fr").doesNotContain("marie@exemple.fr");
    }

    @Test
    void nEcritJamaisLeLienQuandLeBouchonEstInactif(CapturedOutput sortie) {
        new EnvoiLienBouchon(new ConnexionProperties(false, "http://ecran")).envoyer("marie@exemple.fr", LIEN);

        assertThat(sortie.getOut()).doesNotContain("secret");
    }
}
