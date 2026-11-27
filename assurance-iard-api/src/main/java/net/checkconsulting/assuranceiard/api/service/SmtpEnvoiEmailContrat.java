package net.checkconsulting.assuranceiard.api.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** Envoie les e-mails liés au contrat via SMTP (Mailpit en local, Brevo en production). */
@Component
@RequiredArgsConstructor
public class SmtpEnvoiEmailContrat implements EnvoiEmailContrat {

    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);

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

    @Override
    public void envoyerMandatSepa(String destinataire, String prenom, String nom, String numeroContrat,
            LocalDate premierPrelevement, BigDecimal montantMensuel, byte[] pdfMandatSepa) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(destinataire);
            helper.setSubject("Votre mandat de prélèvement SEPA et votre échéancier – Assur'Easy");

            String montantFormate = String.format(Locale.FRANCE, "%.2f", montantMensuel);
            helper.setText(
                    "Bonjour " + prenom + " " + nom + ",\n\n"
                    + "Vous trouverez ci-joint votre mandat de prélèvement SEPA et\n"
                    + "l'échéancier de vos cotisations pour votre contrat auto n° " + numeroContrat + ".\n"
                    + "Le premier prélèvement aura lieu le " + premierPrelevement.format(FORMAT_DATE)
                    + ", pour un montant de " + montantFormate + " €.\n\n"
                    + "Ce document vaut pré-notification des prélèvements SEPA.\n"
                    + "Votre contrat est en attente de signature : retrouvez-le dans votre espace client.\n\n"
                    + "L'équipe Assur'Easy");

            helper.addAttachment("mandat-sepa-echeancier-" + numeroContrat + ".pdf",
                    () -> new java.io.ByteArrayInputStream(pdfMandatSepa), "application/pdf");
            mailSender.send(message);
        } catch (MessagingException erreur) {
            throw new IllegalStateException("Erreur construction e-mail mandat SEPA", erreur);
        }
    }
}
