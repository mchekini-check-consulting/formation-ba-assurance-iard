package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SmtpEnvoiCodeTest {

    @Mock
    private JavaMailSender mailSender;

    private SmtpEnvoiCode service;
    private MimeMessage message;

    @BeforeEach
    void preparer() {
        message = new MimeMessage(Session.getInstance(new Properties()));
        service = new SmtpEnvoiCode(mailSender);
        ReflectionTestUtils.setField(service, "mailFrom", "noreply@assureasy.fr");
    }

    @Test
    void envoieLeCodeAuCompteDepuisLAdresseNoReplyAssurEasy() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(message);
        service.envoyer("marie@exemple.fr", "482913");

        verify(mailSender).send(message);
        message.saveChanges();
        InternetAddress expediteur = (InternetAddress) message.getFrom()[0];
        assertThat(expediteur.getAddress()).isEqualTo("noreply@assureasy.fr");
        assertThat(expediteur.getPersonal()).isEqualTo("Assur'Easy");
        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly("marie@exemple.fr");
        assertThat(message.getSubject()).isEqualTo("Votre code de connexion Assur'Easy");
    }

    @Test
    void contientLeCodeLaDureeEtLaPhraseSiLaConnexionNEstPasDemandee() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(message);
        service.envoyer("marie@exemple.fr", "482913");
        message.saveChanges();

        List<String> parties = new ArrayList<>();
        lireParties(message, parties);

        assertThat(parties).hasSize(2).allSatisfy(partie -> assertThat(partie)
                .contains("482913")
                .contains("10 minutes")
                .contains("Si vous n'avez pas tenté de vous connecter, ne tenez pas compte de ce message."));
    }

    @Test
    void laVersionHtmlEstAuxCouleursRougeEtBlancDAssurEasy() {
        String html = SmtpEnvoiCode.html("482913");

        assertThat(html)
                .contains("background-color:#A82B27;color:#ffffff")
                .contains("background-color:#ffffff")
                .contains("482913")
                .doesNotContain("%1$s", "%2$s", "%3$s");
    }

    private static void lireParties(Part partie, List<String> textes) throws Exception {
        Object contenu = partie.getContent();
        if (contenu instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart sousPartie = multipart.getBodyPart(i);
                lireParties(sousPartie, textes);
            }
        } else if (contenu instanceof String texte) {
            textes.add(texte);
        }
    }
}
