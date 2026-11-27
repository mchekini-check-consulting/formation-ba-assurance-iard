package net.checkconsulting.assuranceiard.api.dto;

/** Résultat de la vérification IBAN. L'IBAN complet n'est jamais renvoyé. */
public record IbanVerificationResponse(
        boolean valide,
        String ibanMasque,
        String bic,
        String banque,
        boolean zoneSepa,
        String messageErreur) {

    public static IbanVerificationResponse valide(String ibanMasque, String bic, String banque) {
        return new IbanVerificationResponse(true, ibanMasque, bic, banque, true, null);
    }

    public static IbanVerificationResponse invalide(String messageErreur) {
        return new IbanVerificationResponse(false, null, null, null, true, messageErreur);
    }

    public static IbanVerificationResponse horsSepa(String messageErreur) {
        return new IbanVerificationResponse(false, null, null, null, false, messageErreur);
    }
}
