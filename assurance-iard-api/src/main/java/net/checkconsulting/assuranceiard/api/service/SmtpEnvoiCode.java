package net.checkconsulting.assuranceiard.api.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * US-06 : envoie le code de connexion par e-mail via SMTP (Mailpit en local, dev et qualif ; SMTP réel en preprod
 * et prod). Version HTML aux couleurs d'Assur'Easy et version texte pour les messageries sans HTML. Le code
 * n'est jamais écrit dans les logs.
 */
@Component
@RequiredArgsConstructor
public class SmtpEnvoiCode implements EnvoiCode {

    static final String EXPEDITEUR = "Assur'Easy";
    static final String OBJET = "Votre code de connexion Assur'Easy";
    static final String ROUGE = "#A82B27";
    static final String MESSAGE_NON_DEMANDE =
            "Si vous n'avez pas tenté de vous connecter, ne tenez pas compte de ce message.";

    private final JavaMailSender mailSender;

    @Value("${assurance.mail.from:noreply@assureasy.fr}")
    private String mailFrom;

    @Override
    public void envoyer(String email, String code) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper contenu = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            contenu.setFrom(mailFrom, EXPEDITEUR);
            contenu.setTo(email);
            contenu.setSubject(OBJET);
            contenu.setText(texte(code), html(code));
        } catch (MessagingException | UnsupportedEncodingException exception) {
            throw new MailPreparationException("Préparation de l'e-mail du code de connexion impossible", exception);
        }
        mailSender.send(message);
    }

    static String texte(String code) {
        return "Bonjour,\n\n"
                + "Votre code de connexion à votre espace Assur'Easy est : " + code + "\n\n"
                + "Ce code est valable 10 minutes. Ne le communiquez à personne.\n\n"
                + MESSAGE_NON_DEMANDE + "\n\n"
                + "L'équipe Assur'Easy";
    }

    static String html(String code) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <body style="margin:0;padding:0;background-color:#ffffff;font-family:Arial,Helvetica,sans-serif;color:#222222;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#ffffff;">
                    <tr>
                      <td align="center" style="padding:24px 16px;">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;border:1px solid %1$s;">
                          <tr>
                            <td style="background-color:%1$s;color:#ffffff;padding:20px 24px;font-size:22px;font-weight:bold;">Assur'Easy</td>
                          </tr>
                          <tr>
                            <td style="padding:24px;font-size:16px;line-height:1.5;">
                              <p style="margin:0 0 16px;">Bonjour,</p>
                              <p style="margin:0 0 16px;">Voici votre code de connexion à votre espace Assur'Easy :</p>
                              <p style="margin:0 0 16px;text-align:center;">
                                <span style="display:inline-block;padding:12px 24px;border:2px solid %1$s;color:%1$s;font-size:32px;font-weight:bold;letter-spacing:8px;">%2$s</span>
                              </p>
                              <p style="margin:0 0 16px;">Ce code est valable <strong>10 minutes</strong>. Ne le communiquez à personne.</p>
                              <p style="margin:0 0 16px;">%3$s</p>
                              <p style="margin:0;">L'équipe Assur'Easy</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="background-color:%1$s;color:#ffffff;padding:12px 24px;font-size:12px;">Ce message est envoyé automatiquement, merci de ne pas y répondre.</td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(ROUGE, code, MESSAGE_NON_DEMANDE);
    }
}
