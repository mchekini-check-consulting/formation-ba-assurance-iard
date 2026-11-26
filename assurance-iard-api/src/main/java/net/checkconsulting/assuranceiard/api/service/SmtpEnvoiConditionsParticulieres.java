package net.checkconsulting.assuranceiard.api.service;

import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.config.ConnexionProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * US-21, AC3 : envoie les conditions particulières régénérées par e-mail via SMTP (Mailpit en local, dev et qualif),
 * PDF en pièce jointe (support durable, RG3, art. L112-2-1) et lien vers la fiche du contrat dans l'espace client.
 * Mise en page commune : {@link GabaritCourriel}. Aucune donnée personnelle n'est écrite dans les logs.
 */
@Component
@RequiredArgsConstructor
public class SmtpEnvoiConditionsParticulieres implements EnvoiConditionsParticulieres {

    static final String EXPEDITEUR = GabaritCourriel.EXPEDITEUR;
    static final String OBJET = "Vos conditions particulières à jour – Assur'Easy";

    private final JavaMailSender mailSender;
    private final ConnexionProperties proprietes;

    @Value("${assurance.mail.from:noreply@assureasy.fr}")
    private String mailFrom;

    @Override
    public void envoyer(String email, String prenom, String numeroContrat, int version, String nomFichier,
            byte[] pdf) {
        String lien = lienFiche(numeroContrat);
        GabaritCourriel.envoyer(mailSender, mailFrom, new GabaritCourriel.Courriel(email, OBJET,
                texte(prenom, numeroContrat, version, lien), html(prenom, numeroContrat, version, lien), nomFichier,
                pdf));
    }

    String lienFiche(String numeroContrat) {
        return proprietes.urlFrontend() + "/espace-client/contrats/" + numeroContrat;
    }

    static String salutation(String prenom) {
        return GabaritCourriel.salutation(prenom);
    }

    static String texte(String prenom, String numeroContrat, int version, String lien) {
        return salutation(prenom) + "\n\n"
                + "Vos conditions particulières à jour pour le contrat " + numeroContrat + " (version " + version
                + ") sont jointes à ce message.\n\n"
                + "Vous les retrouvez aussi, avec les versions précédentes, dans votre espace client :\n" + lien
                + "\n\n"
                + GabaritCourriel.SIGNATURE;
    }

    static String html(String prenom, String numeroContrat, int version, String lien) {
        return GabaritCourriel.html(prenom,
                "Vos conditions particulières à jour pour le contrat <strong>" + HtmlUtils.htmlEscape(numeroContrat)
                        + "</strong> (version " + version + ") sont jointes à ce message.",
                "Vous les retrouvez aussi, avec les versions précédentes, dans "
                        + GabaritCourriel.lienEspaceClient(lien) + ".");
    }
}
