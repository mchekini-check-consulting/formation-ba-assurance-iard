package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.checkconsulting.assuranceiard.api.config.ReinitialisationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SmtpEnvoiLienReinitialisationTest {

    private static final String LIEN = "https://frontend.qualif/reinitialisation-mot-de-passe?jeton=abc_DEF-123";

    @Mock
    private JavaMailSender mailSender;

    private MimeMessage message;

    @BeforeEach
    void preparer() {
        message = new MimeMessage(Session.getInstance(new Properties()));
    }

    private SmtpEnvoiLienReinitialisation service(Duration validite) {
        SmtpEnvoiLienReinitialisation service =
                new SmtpEnvoiLienReinitialisation(mailSender, new ReinitialisationProperties(validite));
        ReflectionTestUtils.setField(service, "mailFrom", "noreply@assureasy.fr");
        return service;
    }

    private void envoyer(Duration validite) {
        when(mailSender.createMimeMessage()).thenReturn(message);
        service(validite).envoyer("marie@exemple.fr", LIEN);
    }

    @Test
    void envoieLeMessagePrepare() {
        envoyer(Duration.ofHours(24));

        verify(mailSender).send(message);
    }

    @Test
    void annonceLaDureeReglee() throws Exception {
        envoyer(Duration.ofMinutes(2));
        message.saveChanges();

        List<String> parties = new ArrayList<>();
        lireParties(message, parties);
        assertThat(parties).allSatisfy(partie -> assertThat(partie).containsPattern("valable (<strong>)?2 minutes"));
    }

    @Test
    void envoieAuCompteDepuisLAdresseNoReplyAssurEasyAvecUnObjetClair() throws Exception {
        envoyer(Duration.ofHours(24));
        message.saveChanges();

        InternetAddress expediteur = (InternetAddress) message.getFrom()[0];
        assertThat(expediteur.getAddress()).isEqualTo("noreply@assureasy.fr");
        assertThat(expediteur.getPersonal()).isEqualTo("Assur'Easy");
        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly("marie@exemple.fr");
        assertThat(message.getSubject()).isEqualTo("Réinitialisation de votre mot de passe Assur'Easy");
    }

    @Test
    void contientLeLienLaDureeEtLaPhraseSiRienNAEteDemande() throws Exception {
        envoyer(Duration.ofHours(24));
        message.saveChanges();

        List<String> parties = new ArrayList<>();
        lireParties(message, parties);

        assertThat(parties).hasSize(2).allSatisfy(partie -> assertThat(partie)
                .contains(LIEN)
                .containsPattern("valable (<strong>)?24 heures")
                .contains("Si vous n'avez pas demandé à réinitialiser votre mot de passe, ne tenez pas compte"));
    }

    @Test
    void laVersionHtmlAUnBoutonVersLeLienAuxCouleursRougeEtBlanc() {
        String html = SmtpEnvoiLienReinitialisation.html(LIEN, "24 heures");

        assertThat(html)
                .contains("<a href=\"" + LIEN + "\"")
                .contains(">Réinitialiser mon mot de passe</a>")
                .contains("background-color:#A82B27;color:#ffffff");
    }

    @Test
    void laDureeAnnonceeSuitLeReglage() {
        assertThat(SmtpEnvoiLienReinitialisation.enToutesLettres(Duration.ofHours(24))).isEqualTo("24 heures");
        assertThat(SmtpEnvoiLienReinitialisation.enToutesLettres(Duration.ofHours(1))).isEqualTo("1 heure");
        assertThat(SmtpEnvoiLienReinitialisation.enToutesLettres(Duration.ofMinutes(2))).isEqualTo("2 minutes");
        assertThat(SmtpEnvoiLienReinitialisation.enToutesLettres(Duration.ofMinutes(90))).isEqualTo("90 minutes");
        assertThat(SmtpEnvoiLienReinitialisation.enToutesLettres(Duration.ofSeconds(30))).isEqualTo("30 secondes");
    }

    private static void lireParties(Part partie, List<String> textes) throws Exception {
        Object contenu = partie.getContent();
        if (contenu instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                lireParties(multipart.getBodyPart(i), textes);
            }
        } else if (contenu instanceof String texte) {
            textes.add(texte);
        }
    }
}
