package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.checkconsulting.assuranceiard.api.config.ConnexionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

class SmtpEnvoiConditionsParticulieresTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
    private SmtpEnvoiConditionsParticulieres service;

    @BeforeEach
    void preparer() {
        service = new SmtpEnvoiConditionsParticulieres(mailSender,
                new ConnexionProperties(false, "https://frontend.exemple.test"));
        ReflectionTestUtils.setField(service, "mailFrom", "noreply@assureasy.fr");
        when(mailSender.createMimeMessage()).thenReturn(message);
    }

    /** Toutes les parties « feuilles » du message (texte, HTML, pièce jointe). */
    private static List<Part> parties(Part part) throws Exception {
        List<Part> resultat = new ArrayList<>();
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart enfant = multipart.getBodyPart(i);
                resultat.addAll(parties(enfant));
            }
        } else {
            resultat.add(part);
        }
        return resultat;
    }

    @Test
    void ac3EnvoieLePdfEnPieceJointeAvecUnLienVersLaFiche() throws Exception {
        service.envoyer("camille@exemple.fr", "Camille", "AE-RECETTE-A03", 3, "conditions-particulieres-v3.pdf",
                new byte[] {'%', 'P', 'D', 'F'});

        verify(mailSender).send(message);
        message.saveChanges();
        InternetAddress expediteur = (InternetAddress) message.getFrom()[0];
        assertThat(expediteur.getAddress()).isEqualTo("noreply@assureasy.fr");
        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly("camille@exemple.fr");
        assertThat(message.getSubject()).isEqualTo(SmtpEnvoiConditionsParticulieres.OBJET);

        List<Part> parties = parties(message);
        Part pieceJointe = parties.stream().filter(p -> "conditions-particulieres-v3.pdf".equals(fichier(p)))
                .findFirst().orElseThrow();
        assertThat(pieceJointe.getContentType()).startsWith("application/pdf");
        ByteArrayOutputStream contenu = new ByteArrayOutputStream();
        pieceJointe.getDataHandler().writeTo(contenu);
        assertThat(contenu.toByteArray()).containsExactly('%', 'P', 'D', 'F');
        String texte = parties.stream().filter(p -> fichier(p) == null).map(SmtpEnvoiConditionsParticulieresTest::texte)
                .reduce("", String::concat);
        assertThat(texte).contains("Bonjour Camille,", "AE-RECETTE-A03", "version 3",
                "https://frontend.exemple.test/espace-client/contrats/AE-RECETTE-A03");
    }

    @Test
    void sansPrenomUneFormuleDAppelSimpleEtLeHtmlEstEchappe() {
        assertThat(SmtpEnvoiConditionsParticulieres.salutation(null)).isEqualTo("Bonjour,");
        assertThat(SmtpEnvoiConditionsParticulieres.salutation("  ")).isEqualTo("Bonjour,");
        assertThat(SmtpEnvoiConditionsParticulieres.html("<b>Eve</b>", "AE-1", 2, "https://x/a?b=1&c=2"))
                .contains("Bonjour &lt;b&gt;Eve&lt;/b&gt;,", "https://x/a?b=1&amp;c=2")
                .doesNotContain("<b>Eve</b>");
    }

    private static String fichier(Part part) {
        try {
            return part.getFileName();
        } catch (jakarta.mail.MessagingException erreur) {
            return null;
        }
    }

    private static String texte(Part part) {
        try {
            return String.valueOf(part.getContent());
        } catch (Exception erreur) {
            return "";
        }
    }
}
