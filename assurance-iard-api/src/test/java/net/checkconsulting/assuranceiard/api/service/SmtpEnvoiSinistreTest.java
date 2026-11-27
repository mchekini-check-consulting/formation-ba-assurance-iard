package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import net.checkconsulting.assuranceiard.api.config.ConnexionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * US-24, AC9 et AC13 : accusé de réception et information du souscripteur (Mailpit). RG commune « Données de santé » :
 * aucun e-mail ne contient de donnée de santé (blessés, description des faits) ni de pièce jointe.
 */
@ExtendWith(MockitoExtension.class)
class SmtpEnvoiSinistreTest {

    /** Mots qui ne doivent jamais apparaître dans un e-mail de sinistre. */
    static final List<String> SANTE = List.of("bless", "médical", "medical", "santé", "hôpital", "hospitalis",
            "Fuite du ballon");

    private static final EnvoiSinistre.Dossier DOSSIER = new EnvoiSinistre.Dossier("camille@exemple.fr", "Camille",
            "SIN-2026-000042", "AE-RECETTE-A03", "Vol du véhicule", LocalDate.of(2026, 11, 24), true, null, null);
    private static final EnvoiSinistre.Dossier PAR_UN_PROCHE = new EnvoiSinistre.Dossier("camille@exemple.fr",
            "Camille", "SIN-2026-000042", "AE-RECETTE-A03", "Vol du véhicule", LocalDate.of(2026, 11, 24), false,
            "Léa Durand", "Fille");

    @Mock
    private JavaMailSender mailSender;

    private final MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
    private SmtpEnvoiSinistre service;

    @BeforeEach
    void preparer() {
        service = new SmtpEnvoiSinistre(mailSender, new ConnexionProperties(false, "https://frontend.exemple.test"));
        ReflectionTestUtils.setField(service, "mailFrom", "noreply@assureasy.fr");
        Mockito.lenient().when(mailSender.createMimeMessage()).thenReturn(message);
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

    private String texte() throws Exception {
        StringBuilder texte = new StringBuilder();
        for (Part partie : parties(message)) {
            texte.append(partie.getContent());
        }
        return texte.toString();
    }

    @Test
    void ac9AccuseDeReceptionAvecNumeroEtProchainesEtapesSansPieceJointe() throws Exception {
        service.accuserReception(DOSSIER);

        verify(mailSender).send(message);
        message.saveChanges();
        assertThat(message.getSubject()).isEqualTo(SmtpEnvoiSinistre.OBJET_ACCUSE);
        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly("camille@exemple.fr");
        assertThat(parties(message)).allSatisfy(partie -> assertThat(partie.getFileName()).isNull());
        assertThat(texte()).contains("Bonjour Camille,", "SIN-2026-000042", "Vol du véhicule", "24/11/2026",
                "AE-RECETTE-A03", "https://frontend.exemple.test/espace-client/sinistres",
                SmtpEnvoiSinistre.PIECES_MANQUANTES);
    }

    @Test
    void ac13LeSouscripteurEstInformeQuUnProcheADeclare() throws Exception {
        service.informerSouscripteur(PAR_UN_PROCHE);

        message.saveChanges();
        assertThat(message.getSubject()).isEqualTo(SmtpEnvoiSinistre.OBJET_INFORMATION);
        assertThat(texte()).contains("Léa Durand (Fille) a déclaré le sinistre SIN-2026-000042",
                SmtpEnvoiSinistre.SEUL_A_ACCEPTER);
    }

    @Test
    void rgSanteAucuneDonneeDeSanteDansLesEMails() {
        List<String> courriels = List.of(
                SmtpEnvoiSinistre.texteAccuse(DOSSIER, "https://x"), SmtpEnvoiSinistre.htmlAccuse(DOSSIER, "https://x"),
                SmtpEnvoiSinistre.texteInformation(PAR_UN_PROCHE, "https://x"),
                SmtpEnvoiSinistre.htmlInformation(PAR_UN_PROCHE, "https://x"));

        courriels.forEach(courriel -> assertThat(courriel.toLowerCase(Locale.ROOT))
                .doesNotContain(SANTE.stream().map(mot -> mot.toLowerCase(Locale.ROOT)).toList()));
    }

    private static int occurrences(String texte, String mot) {
        return texte.split(java.util.regex.Pattern.quote(mot), -1).length - 1;
    }

    @Test
    void recetteUneSeuleFoisVotreEspaceClientDansChaqueEMailTexteEtHtml() {
        String lien = "https://frontend.exemple.test/espace-client/sinistres";
        String texteAccuse = SmtpEnvoiSinistre.texteAccuse(DOSSIER, lien);
        String htmlAccuse = org.springframework.web.util.HtmlUtils.htmlUnescape(SmtpEnvoiSinistre.htmlAccuse(DOSSIER, lien));
        String texteInformation = SmtpEnvoiSinistre.texteInformation(PAR_UN_PROCHE, lien);
        String htmlInformation = org.springframework.web.util.HtmlUtils.htmlUnescape(
                SmtpEnvoiSinistre.htmlInformation(PAR_UN_PROCHE, lien));

        assertThat(texteAccuse).contains("Vous suivez son avancement dans « Mes sinistres » de votre espace client : "
                + lien);
        assertThat(htmlAccuse).contains("Vous suivez son avancement dans « Mes sinistres » de <a href=\"" + lien
                + "\" style=\"color:#A82B27;\">votre espace client</a>.");
        assertThat(texteInformation).contains("Votre dossier est dans « Mes sinistres » de votre espace client : "
                + lien);
        assertThat(htmlInformation).contains("Votre dossier est dans « Mes sinistres » de <a href=\"" + lien
                + "\" style=\"color:#A82B27;\">votre espace client</a>.");
        for (String courriel : List.of(texteAccuse, htmlAccuse, texteInformation, htmlInformation)) {
            assertThat(occurrences(courriel, "votre espace client")).isEqualTo(1);
            assertThat(courriel).doesNotContain("espace client. votre espace client", "espace client.\n\n" + lien);
        }
        assertThat(occurrences(htmlAccuse, "<a href=")).isEqualTo(1);
        assertThat(occurrences(htmlInformation, "<a href=")).isEqualTo(1);
    }

    @Test
    void sansPieceManquanteLeRappelNApparaitPas() {
        EnvoiSinistre.Dossier complet = new EnvoiSinistre.Dossier("c@x.fr", null, "SIN-2026-000001", "AE-1",
                "Incendie", LocalDate.of(2026, 11, 20), false, null, null);

        assertThat(SmtpEnvoiSinistre.texteAccuse(complet, "https://x")).startsWith("Bonjour,")
                .doesNotContain(SmtpEnvoiSinistre.PIECES_MANQUANTES);
        assertThat(SmtpEnvoiSinistre.htmlAccuse(complet, "https://x")).doesNotContain("Pensez à déposer");
    }
}
