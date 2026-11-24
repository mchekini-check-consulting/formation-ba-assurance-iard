package net.checkconsulting.assuranceiard.api.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.config.ConnexionProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * US-21, AC3 : envoie les conditions particulières régénérées par e-mail via SMTP (Mailpit en local, dev et qualif),
 * PDF en pièce jointe (support durable, RG3, art. L112-2-1) et lien vers la fiche du contrat dans l'espace client.
 * Aucune donnée personnelle n'est écrite dans les logs.
 */
@Component
@RequiredArgsConstructor
public class SmtpEnvoiConditionsParticulieres implements EnvoiConditionsParticulieres {

    static final String EXPEDITEUR = "Assur'Easy";
    static final String OBJET = "Vos conditions particulières à jour – Assur'Easy";

    private final JavaMailSender mailSender;
    private final ConnexionProperties proprietes;

    @Value("${assurance.mail.from:noreply@assureasy.fr}")
    private String mailFrom;

    @Override
    public void envoyer(String email, String prenom, String numeroContrat, int version, String nomFichier,
            byte[] pdf) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper contenu = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            contenu.setFrom(mailFrom, EXPEDITEUR);
            contenu.setTo(email);
            contenu.setSubject(OBJET);
            String lien = lienFiche(numeroContrat);
            contenu.setText(texte(prenom, numeroContrat, version, lien), html(prenom, numeroContrat, version, lien));
            contenu.addAttachment(nomFichier, new ByteArrayResource(pdf), "application/pdf");
        } catch (MessagingException | UnsupportedEncodingException exception) {
            throw new MailPreparationException("Préparation de l'e-mail des conditions particulières impossible",
                    exception);
        }
        mailSender.send(message);
    }

    String lienFiche(String numeroContrat) {
        return proprietes.urlFrontend() + "/espace-client/contrats/" + numeroContrat;
    }

    static String salutation(String prenom) {
        return prenom == null || prenom.isBlank() ? "Bonjour," : "Bonjour " + prenom.trim() + ",";
    }

    static String texte(String prenom, String numeroContrat, int version, String lien) {
        return salutation(prenom) + "\n\n"
                + "Vos conditions particulières à jour pour le contrat " + numeroContrat + " (version " + version
                + ") sont jointes à ce message.\n\n"
                + "Vous les retrouvez aussi, avec les versions précédentes, dans votre espace client :\n" + lien
                + "\n\n"
                + "L'équipe Assur'Easy";
    }

    static String html(String prenom, String numeroContrat, int version, String lien) {
        String lienEchappe = HtmlUtils.htmlEscape(lien);
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <body style="margin:0;padding:24px 16px;font-family:Arial,Helvetica,sans-serif;color:#222222;">
                  <div style="max-width:520px;margin:0 auto;border:1px solid #A82B27;">
                    <div style="background-color:#A82B27;color:#ffffff;padding:20px 24px;font-size:22px;font-weight:bold;">Assur'Easy</div>
                    <div style="padding:24px;font-size:16px;line-height:1.5;">
                      <p style="margin:0 0 16px;">%s</p>
                      <p style="margin:0 0 16px;">Vos conditions particulières à jour pour le contrat <strong>%s</strong> (version %d) sont jointes à ce message.</p>
                      <p style="margin:0 0 16px;">Vous les retrouvez aussi, avec les versions précédentes, dans <a href="%s" style="color:#A82B27;">votre espace client</a>.</p>
                      <p style="margin:0;">L'équipe Assur'Easy</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(salutation(prenom)), HtmlUtils.htmlEscape(numeroContrat), version,
                lienEchappe);
    }
}
