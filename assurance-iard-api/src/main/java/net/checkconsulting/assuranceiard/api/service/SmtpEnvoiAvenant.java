package net.checkconsulting.assuranceiard.api.service;

import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.config.ConnexionProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * US-23, AC4 : envoie l'avenant accepté par e-mail via SMTP (Mailpit en local, dev et qualif), PDF en pièce jointe et
 * lien vers la fiche du contrat, comme les conditions particulières d'US-21 ({@link GabaritCourriel}). Aucune donnée
 * personnelle n'est écrite dans les logs.
 */
@Component
@RequiredArgsConstructor
public class SmtpEnvoiAvenant implements EnvoiAvenant {

    static final String OBJET = "Votre avenant accepté – Assur'Easy";

    private final JavaMailSender mailSender;
    private final ConnexionProperties proprietes;

    @Value("${assurance.mail.from:noreply@assureasy.fr}")
    private String mailFrom;

    @Override
    public void envoyer(String email, String prenom, String numeroContrat, String numeroAvenant, String nomFichier,
            byte[] pdf) {
        String lien = proprietes.urlFrontend() + "/espace-client/contrats/" + numeroContrat;
        GabaritCourriel.envoyer(mailSender, mailFrom, new GabaritCourriel.Courriel(email, OBJET,
                texte(prenom, numeroContrat, numeroAvenant, lien), html(prenom, numeroContrat, numeroAvenant, lien),
                nomFichier, pdf));
    }

    static String texte(String prenom, String numeroContrat, String numeroAvenant, String lien) {
        return GabaritCourriel.salutation(prenom) + "\n\n"
                + "Vous avez accepté l'avenant " + numeroAvenant + " de votre contrat " + numeroContrat
                + ". Il est joint à ce message.\n\n"
                + "Vos nouvelles garanties et l'historique de vos avenants sont dans votre espace client :\n" + lien
                + "\n\n"
                + GabaritCourriel.SIGNATURE;
    }

    static String html(String prenom, String numeroContrat, String numeroAvenant, String lien) {
        return GabaritCourriel.html(prenom,
                "Vous avez accepté l'avenant <strong>" + HtmlUtils.htmlEscape(numeroAvenant)
                        + "</strong> de votre contrat <strong>" + HtmlUtils.htmlEscape(numeroContrat)
                        + "</strong>. Il est joint à ce message.",
                "Vos nouvelles garanties et l'historique de vos avenants sont dans "
                        + GabaritCourriel.lienEspaceClient(lien) + ".");
    }
}
