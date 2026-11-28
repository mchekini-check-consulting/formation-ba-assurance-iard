package net.checkconsulting.assuranceiard.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Envoie les e-mails liés au contrat (US-16, US-31). */
public interface EnvoiEmailContrat {

    void envoyer(String destinataire, String code);

    /**
     * US-31 étape 5, RG6 : remise sur support durable — envoie le mandat SEPA et l'échéancier en PDF.
     * L'e-mail ne contient jamais l'IBAN, même masqué.
     */
    void envoyerMandatSepa(String destinataire, String prenom, String nom, String numeroContrat,
            LocalDate premierPrelevement, BigDecimal montantMensuel, byte[] pdfMandatSepa);

    /** US-33 : confirmation de signature — envoie les CP et le mémo véhicule en PJ. */
    void envoyerConfirmationSignature(String destinataire, String prenom, String nom, String numeroContrat,
            byte[] pdfConditionsParticulieres, byte[] pdfMemoVehicule);
}
