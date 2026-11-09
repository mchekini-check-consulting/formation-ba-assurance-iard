package net.checkconsulting.assuranceiard.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Envoie le code de signature par e-mail via SMTP (Mailpit en local, Brevo en production). */
@Component
@RequiredArgsConstructor
public class SmtpEnvoiEmailContrat implements EnvoiEmailContrat {

    private final JavaMailSender mailSender;

    @Value("${assurance.mail.from:noreply@assureasy.fr}")
    private String mailFrom;

    @Override
    public void envoyer(String destinataire, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(destinataire);
        message.setSubject("Votre code de signature — Assur'Easy");
        message.setText(
                "Bonjour,\n\n"
                + "Votre code de signature est : " + code + "\n\n"
                + "Ce code est valable 10 minutes. Ne le communiquez à personne.\n\n"
                + "L'équipe Assur'Easy");
        mailSender.send(message);
    }
}
