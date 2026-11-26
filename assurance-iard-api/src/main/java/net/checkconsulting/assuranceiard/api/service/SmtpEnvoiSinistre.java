package net.checkconsulting.assuranceiard.api.service;

import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.config.ConnexionProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * US-24 : e-mails de la déclaration de sinistre par SMTP (Mailpit en local, dev et qualif), mis en page par
 * {@link GabaritCourriel}. Ils ne contiennent que le numéro, le type, la date et les prochaines étapes : jamais la
 * description des faits, les blessés ni aucune donnée de santé (RGPD, article 9), et aucune pièce jointe.
 */
@Component
@RequiredArgsConstructor
public class SmtpEnvoiSinistre implements EnvoiSinistre {

    static final String OBJET_ACCUSE = "Accusé de réception de votre déclaration de sinistre – Assur'Easy";
    static final String OBJET_INFORMATION = "Un sinistre a été déclaré pour votre contrat – Assur'Easy";
    /** Suivie de « votre espace client » (lien en HTML, adresse en texte), une seule fois (recette US-24). */
    static final String PROCHAINES_ETAPES = "Un gestionnaire étudie votre dossier et vous contactera si besoin. "
            + "Vous suivez son avancement dans « Mes sinistres » de ";
    static final String DOSSIER_DANS = "Votre dossier est dans « Mes sinistres » de ";
    static final String ESPACE_CLIENT = "votre espace client";
    static final String PIECES_MANQUANTES = "Pensez à déposer les justificatifs obligatoires qui manquent encore : "
            + "votre dossier sera instruit plus vite.";
    static final String SEUL_A_ACCEPTER = "Vous restez le seul à pouvoir accepter une offre d'indemnisation.";
    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String CHEMIN = "/espace-client/sinistres";

    private final JavaMailSender mailSender;
    private final ConnexionProperties proprietes;

    @Value("${assurance.mail.from:noreply@assureasy.fr}")
    private String mailFrom;

    @Override
    public void accuserReception(Dossier dossier) {
        envoyer(dossier, OBJET_ACCUSE, texteAccuse(dossier, lien()), htmlAccuse(dossier, lien()));
    }

    @Override
    public void informerSouscripteur(Dossier dossier) {
        envoyer(dossier, OBJET_INFORMATION, texteInformation(dossier, lien()), htmlInformation(dossier, lien()));
    }

    private String lien() {
        return proprietes.urlFrontend() + CHEMIN;
    }

    private void envoyer(Dossier dossier, String objet, String texte, String html) {
        GabaritCourriel.envoyer(mailSender, mailFrom,
                new GabaritCourriel.Courriel(dossier.email(), objet, texte, html, null, null));
    }

    static String resume(Dossier dossier) {
        return "Votre déclaration " + dossier.numeroSinistre() + " (" + dossier.libelleType() + " du "
                + dossier.dateSurvenance().format(JJ_MM_AAAA) + ", contrat " + dossier.numeroContrat()
                + ") a bien été reçue.";
    }

    /** « … de votre espace client : https://… » (texte brut : l'adresse suit, une seule fois). */
    static String versEspaceClient(String debut, String lien) {
        return debut + ESPACE_CLIENT + " : " + lien;
    }

    /** « … de <a>votre espace client</a>. » (HTML : le lien porte les mots, une seule fois). */
    static String versEspaceClientHtml(String debut, String lien) {
        return HtmlUtils.htmlEscape(debut) + GabaritCourriel.lienEspaceClient(lien) + ".";
    }

    static String texteAccuse(Dossier dossier, String lien) {
        return GabaritCourriel.salutation(dossier.prenom()) + "\n\n" + resume(dossier) + "\n\n"
                + versEspaceClient(PROCHAINES_ETAPES, lien)
                + (dossier.piecesManquantes() ? "\n\n" + PIECES_MANQUANTES : "") + "\n\n"
                + GabaritCourriel.SIGNATURE;
    }

    static String htmlAccuse(Dossier dossier, String lien) {
        String recu = HtmlUtils.htmlEscape(resume(dossier));
        String suite = versEspaceClientHtml(PROCHAINES_ETAPES, lien);
        return dossier.piecesManquantes()
                ? GabaritCourriel.html(dossier.prenom(), recu, suite, HtmlUtils.htmlEscape(PIECES_MANQUANTES))
                : GabaritCourriel.html(dossier.prenom(), recu, suite);
    }

    static String information(Dossier dossier) {
        return dossier.declarantNom() + " (" + dossier.declarantLien() + ") a déclaré le sinistre "
                + dossier.numeroSinistre() + " (" + dossier.libelleType() + " du "
                + dossier.dateSurvenance().format(JJ_MM_AAAA) + ") pour votre contrat " + dossier.numeroContrat()
                + ".";
    }

    static String texteInformation(Dossier dossier, String lien) {
        return GabaritCourriel.salutation(dossier.prenom()) + "\n\n" + information(dossier) + "\n\n"
                + SEUL_A_ACCEPTER + "\n\n" + versEspaceClient(DOSSIER_DANS, lien) + "\n\n"
                + GabaritCourriel.SIGNATURE;
    }

    static String htmlInformation(Dossier dossier, String lien) {
        return GabaritCourriel.html(dossier.prenom(), HtmlUtils.htmlEscape(information(dossier)),
                HtmlUtils.htmlEscape(SEUL_A_ACCEPTER), versEspaceClientHtml(DOSSIER_DANS, lien));
    }
}
