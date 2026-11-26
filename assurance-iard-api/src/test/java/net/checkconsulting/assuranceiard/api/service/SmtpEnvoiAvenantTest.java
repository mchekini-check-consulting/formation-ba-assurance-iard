package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.checkconsulting.assuranceiard.api.config.ConnexionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

/** US-23, AC4 : l'avenant part par e-mail (Mailpit), PDF en pièce jointe et lien vers le contrat. */
@ExtendWith(MockitoExtension.class)
class SmtpEnvoiAvenantTest {

    @Mock
    private JavaMailSender mailSender;

    private final MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
    private SmtpEnvoiAvenant service;

    @BeforeEach
    void preparer() {
        service = new SmtpEnvoiAvenant(mailSender, new ConnexionProperties(false, "https://frontend.exemple.test"));
        ReflectionTestUtils.setField(service, "mailFrom", "noreply@assureasy.fr");
        org.mockito.Mockito.lenient().when(mailSender.createMimeMessage()).thenReturn(message);
    }

    private static List<Part> parties(Part part) throws Exception {
        List<Part> resultat = new ArrayList<>();
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                resultat.addAll(parties(multipart.getBodyPart(i)));
            }
        } else {
            resultat.add(part);
        }
        return resultat;
    }

    @Test
    void ac4PdfJointEtLienVersLaFicheDuContrat() throws Exception {
        service.envoyer("camille@exemple.fr", "Camille", "AE-RECETTE-A03", "AV-2026-000009",
                "avenant-AE-RECETTE-A03-v1.pdf", new byte[] {'%', 'P', 'D', 'F'});

        verify(mailSender).send(message);
        message.saveChanges();
        assertThat(message.getSubject()).isEqualTo(SmtpEnvoiAvenant.OBJET);
        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly("camille@exemple.fr");
        List<Part> parties = parties(message);
        assertThat(parties).anySatisfy(partie -> {
            assertThat(partie.getFileName()).isEqualTo("avenant-AE-RECETTE-A03-v1.pdf");
            assertThat(partie.getContentType()).startsWith("application/pdf");
        });
        String texte = parties.stream().filter(partie -> {
            try {
                return partie.getFileName() == null;
            } catch (jakarta.mail.MessagingException erreur) {
                return false;
            }
        }).map(partie -> {
            try {
                return partie.getContent().toString();
            } catch (Exception erreur) {
                return "";
            }
        }).reduce("", String::concat);
        assertThat(texte).contains("Bonjour Camille,", "AV-2026-000009", "AE-RECETTE-A03",
                "https://frontend.exemple.test/espace-client/contrats/AE-RECETTE-A03");
    }

    @Test
    void leHtmlEstEchappe() {
        assertThat(SmtpEnvoiAvenant.html("<b>Eve</b>", "AE-1", "AV-<2026>", "https://x/a?b=1&c=2"))
                .contains("Bonjour &lt;b&gt;Eve&lt;/b&gt;,", "AV-&lt;2026&gt;", "https://x/a?b=1&amp;c=2",
                        "L'équipe Assur'Easy")
                .doesNotContain("<b>Eve</b>");
    }
}
