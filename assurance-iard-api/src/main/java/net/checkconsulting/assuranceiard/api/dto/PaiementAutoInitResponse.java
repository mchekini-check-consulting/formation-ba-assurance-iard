package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Données initiales de l'étape 5 : souscripteur, véhicule, formule, tarif, loi Hamon. */
public record PaiementAutoInitResponse(
        String souscripteurPrenom,
        String souscripteurNom,
        String souscripteurAdresse,
        String souscripteurCodePostal,
        String souscripteurVille,
        String souscripteurEmail,
        String vehiculeMarque,
        String vehiculeModele,
        String vehiculePlaque,
        String formule,
        List<OptionPrix> options,
        BigDecimal franchise,
        BigDecimal prixMensuelTtc,
        BigDecimal prixAnnuelTtc,
        boolean hamonApplicable,
        LocalDate dateFinAncienContrat,
        String ics,
        String creancier,
        String titulairePrerempli,
        String numeroContratExistant,
        Long documentMandatSepaIdExistant) {

    public record OptionPrix(String code, BigDecimal prixMensuelTtc) {
    }
}
