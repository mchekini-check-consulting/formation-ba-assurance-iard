package net.checkconsulting.assuranceiard.api.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.util.HtmlUtils;

/**
 * US-21 / US-23 : mise en page commune des e-mails qui joignent un document PDF (conditions particulières,
 * avenant) : en-tête Assur'Easy, formule d'appel, paragraphes, signature. Aucune donnée personnelle dans les logs.
 */
public final class GabaritCourriel {

    static final String EXPEDITEUR = "Assur'Easy";
    static final String SIGNATURE = "L'équipe Assur'Easy";
    private static final String PARAGRAPHE = "<p style=\"margin:0 0 16px;\">%s</p>";

    private GabaritCourriel() {
    }

    /** Un e-mail prêt à partir, avec son PDF joint. */
    public record Courriel(String email, String objet, String texte, String html, String nomFichier, byte[] pdf) {
    }

    public static void envoyer(JavaMailSender mailSender, String expediteur, Courriel courriel) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper contenu = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            contenu.setFrom(expediteur, EXPEDITEUR);
            contenu.setTo(courriel.email());
            contenu.setSubject(courriel.objet());
            contenu.setText(courriel.texte(), courriel.html());
            contenu.addAttachment(courriel.nomFichier(), new ByteArrayResource(courriel.pdf()), "application/pdf");
        } catch (MessagingException | UnsupportedEncodingException exception) {
            throw new MailPreparationException("Préparation de l'e-mail impossible : " + courriel.objet(), exception);
        }
        mailSender.send(message);
    }

    static String salutation(String prenom) {
        return prenom == null || prenom.isBlank() ? "Bonjour," : "Bonjour " + prenom.trim() + ",";
    }

    /** Lien HTML vers l'espace client (adresse échappée). */
    static String lienEspaceClient(String lien) {
        return "<a href=\"" + HtmlUtils.htmlEscape(lien) + "\" style=\"color:#A82B27;\">votre espace client</a>";
    }

    /**
     * Le corps HTML : formule d'appel (échappée), puis les paragraphes, déjà en HTML (les valeurs qu'ils contiennent
     * sont échappées par l'appelant), puis la signature.
     */
    static String html(String prenom, String... paragraphes) {
        String corps = Arrays.stream(paragraphes).map(PARAGRAPHE::formatted).collect(Collectors.joining("\n"));
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <body style="margin:0;padding:24px 16px;font-family:Arial,Helvetica,sans-serif;color:#222222;">
                  <div style="max-width:520px;margin:0 auto;border:1px solid #A82B27;">
                    <div style="background-color:#A82B27;color:#ffffff;padding:20px 24px;font-size:22px;font-weight:bold;">Assur'Easy</div>
                    <div style="padding:24px;font-size:16px;line-height:1.5;">
                      %s
                      %s
                      <p style="margin:0;">L'équipe Assur'Easy</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(PARAGRAPHE.formatted(HtmlUtils.htmlEscape(salutation(prenom))), corps);
    }
}
