package net.checkconsulting.assuranceiard.api.service;

import net.checkconsulting.assuranceiard.api.dto.IbanVerificationResponse;
import org.springframework.stereotype.Component;

/**
 * US-31 étape 5, RG2 : bouchon de vérification bancaire.
 * Remplace l'appel à un service bancaire réel jusqu'à l'intégration.
 * L'IBAN complet n'est jamais journalisé ni renvoyé.
 */
@Component
public class IbanBouchon {

    private static final String IBAN_VALIDE_SPECIFIQUE = "FR7630006000011234567890189";
    private static final String IBAN_HORS_SEPA = "BR1800360305000010009795493C1";

    private static final String BIC_SPECIFIQUE = "BNPAFRPPXXX";
    private static final String BANQUE_SPECIFIQUE = "BNP Paribas";
    private static final String BIC_GENERIQUE = "TESTBICX";
    private static final String BANQUE_GENERIQUE = "Banque de test";

    private static final String MSG_HORS_SEPA =
            "Cet IBAN n'est pas dans la zone SEPA : le prélèvement est impossible.";

    /** Vérifie si la zone SEPA est supportée pour l'IBAN donné (normalisé, clé déjà vérifiée). */
    public IbanVerificationResponse verifier(String ibanNormalise) {
        if (IBAN_HORS_SEPA.equalsIgnoreCase(ibanNormalise)) {
            return IbanVerificationResponse.horsSepa(MSG_HORS_SEPA);
        }
        if (IBAN_VALIDE_SPECIFIQUE.equalsIgnoreCase(ibanNormalise)) {
            return IbanVerificationResponse.valide(masquer(ibanNormalise), BIC_SPECIFIQUE, BANQUE_SPECIFIQUE);
        }
        return IbanVerificationResponse.valide(masquer(ibanNormalise), BIC_GENERIQUE, BANQUE_GENERIQUE);
    }

    private static String masquer(String iban) {
        return "•••• " + iban.substring(iban.length() - 4);
    }
}
