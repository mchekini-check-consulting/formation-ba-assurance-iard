package net.checkconsulting.assuranceiard.api.service;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** RG2 de l'US-14 : format et clé de contrôle de l'IBAN. Ne renvoie ni n'écrit jamais l'IBAN complet. */
@Service
public class IbanService {

    private static final Pattern FORMAT = Pattern.compile("[A-Z]{2}\\d{2}[A-Z0-9]{11,30}");
    private static final Pattern ESPACES = Pattern.compile("\\s");
    private static final String PAYS_FRANCE = "FR";
    private static final int LONGUEUR_FRANCE = 27;
    private static final int LONGUEUR_ENTETE = 4;
    private static final int LONGUEUR_FIN = 4;
    private static final int MODULE = 97;
    private static final int RESTE_ATTENDU = 1;
    private static final int DIZAINE = 10;
    private static final int CENTAINE = 100;

    public String normaliser(String iban) {
        return ESPACES.matcher(iban).replaceAll("").toUpperCase(Locale.ROOT);
    }

    /** Attend un IBAN normalisé. */
    public boolean valide(String iban) {
        return FORMAT.matcher(iban).matches() && longueurValide(iban) && cleValide(iban);
    }

    public String fin(String iban) {
        return iban.substring(iban.length() - LONGUEUR_FIN);
    }

    public String debut(String iban) {
        return iban.substring(0, LONGUEUR_ENTETE);
    }

    private boolean longueurValide(String iban) {
        return !iban.startsWith(PAYS_FRANCE) || iban.length() == LONGUEUR_FRANCE;
    }

    // Modulo 97 : les 4 premiers caractères passent à la fin, chaque lettre devient un nombre de 10 à 35
    private boolean cleValide(String iban) {
        String reordonne = iban.substring(LONGUEUR_ENTETE) + iban.substring(0, LONGUEUR_ENTETE);
        int reste = 0;
        for (char caractere : reordonne.toCharArray()) {
            int valeur = Character.getNumericValue(caractere);
            int multiplicateur = valeur < DIZAINE ? DIZAINE : CENTAINE;
            reste = (reste * multiplicateur + valeur) % MODULE;
        }
        return reste == RESTE_ATTENDU;
    }
}
