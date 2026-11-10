package net.checkconsulting.assuranceiard.api.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import net.checkconsulting.assuranceiard.api.config.ReinitialisationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * US-07 : envoie le lien de réinitialisation du mot de passe par e-mail via SMTP (Mailpit en local, dev et qualif ;
 * SMTP réel en preprod et prod). Appelé en arrière-plan par {@link ReinitialisationService#demander}. Ni l'adresse
 * ni le lien ne sont écrits dans les logs.
 */
@Component
public class SmtpEnvoiLienReinitialisation implements EnvoiLienReinitialisation {

    static final String EXPEDITEUR = "Assur'Easy";
    static final String OBJET = "Réinitialisation de votre mot de passe Assur'Easy";
    static final String BOUTON = "Réinitialiser mon mot de passe";
    static final String ROUGE = "#A82B27";
    static final String BLANC = "#ffffff";
    static final String MESSAGE_NON_DEMANDE =
            "Si vous n'avez pas demandé à réinitialiser votre mot de passe, ne tenez pas compte de ce message : "
                    + "votre mot de passe actuel reste valable.";

    private final JavaMailSender mailSender;
    private final ReinitialisationProperties reglages;

    @Value("${assurance.mail.from:noreply@assureasy.fr}")
    private String mailFrom;

    public SmtpEnvoiLienReinitialisation(JavaMailSender mailSender, ReinitialisationProperties reglages) {
        this.mailSender = mailSender;
        this.reglages = reglages;
    }

    @Override
    public void envoyer(String email, String lien) {
        mailSender.send(preparer(email, lien));
    }

    private MimeMessage preparer(String email, String lien) {
        String validite = enToutesLettres(reglages.validiteLien());
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper contenu = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            contenu.setFrom(mailFrom, EXPEDITEUR);
            contenu.setTo(email);
            contenu.setSubject(OBJET);
            contenu.setText(texte(lien, validite), html(lien, validite));
        } catch (MessagingException | UnsupportedEncodingException exception) {
            throw new MailPreparationException("Préparation de l'e-mail de réinitialisation impossible", exception);
        }
        return message;
    }

    /** « 24 heures », « 1 heure », « 2 minutes »… pour la phrase de validité de l'e-mail. */
    static String enToutesLettres(Duration duree) {
        if (duree.toMinutes() > 0 && duree.toMinutesPart() == 0 && duree.toSecondsPart() == 0) {
            return pluriel(duree.toHours(), "heure");
        }
        if (duree.toSeconds() >= 60 && duree.toSecondsPart() == 0) {
            return pluriel(duree.toMinutes(), "minute");
        }
        return pluriel(duree.toSeconds(), "seconde");
    }

    private static String pluriel(long nombre, String unite) {
        return nombre + " " + unite + (nombre > 1 ? "s" : "");
    }

    static String texte(String lien, String validite) {
        return "Bonjour,\n\n"
                + "Vous avez demandé à réinitialiser le mot de passe de votre espace Assur'Easy.\n"
                + "Pour choisir un nouveau mot de passe, ouvrez ce lien :\n" + lien + "\n\n"
                + "Ce lien est valable " + validite + " et ne peut servir qu'une seule fois.\n\n"
                + MESSAGE_NON_DEMANDE + "\n\n"
                + "L'équipe Assur'Easy";
    }

    static String html(String lien, String validite) {
        String corps = paragraphe("Bonjour,")
                + paragraphe("Vous avez demandé à réinitialiser le mot de passe de votre espace Assur'Easy.")
                + "<p style=\"margin:24px 0;text-align:center;\"><a href=\"" + lien + "\" style=\"display:inline-block;"
                + "padding:14px 28px;background-color:" + ROUGE + ";color:" + BLANC + ";font-weight:bold;"
                + "text-decoration:none;border-radius:4px;\">" + BOUTON + "</a></p>"
                + paragraphe("Ce lien est valable <strong>" + validite + "</strong> et ne peut servir qu'une seule fois.")
                + paragraphe(MESSAGE_NON_DEMANDE)
                + paragraphe("L'équipe Assur'Easy");
        return "<!DOCTYPE html><html lang=\"fr\"><body style=\"margin:0;padding:0;background-color:" + BLANC
                + ";font-family:Arial,Helvetica,sans-serif;color:#222222;\">"
                + "<div style=\"max-width:520px;margin:24px auto;border:1px solid " + ROUGE + ";\">"
                + "<div style=\"background-color:" + ROUGE + ";color:" + BLANC
                + ";padding:20px 24px;font-size:22px;font-weight:bold;\">Assur'Easy</div>"
                + "<div style=\"padding:24px;font-size:16px;line-height:1.5;background-color:" + BLANC + ";\">" + corps
                + "</div>"
                + "<div style=\"background-color:" + ROUGE + ";color:" + BLANC + ";padding:12px 24px;font-size:12px;\">"
                + "Ce message est envoyé automatiquement, merci de ne pas y répondre.</div>"
                + "</div></body></html>";
    }

    private static String paragraphe(String texte) {
        return "<p style=\"margin:0 0 16px;\">" + texte + "</p>";
    }
}
