package net.checkconsulting.assuranceiard.api.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * RG3 et RG4 de l'US-14 : le premier prélèvement tombe le premier jour choisi qui soit à la fois à partir
 * de la date de début et au moins 14 jours après la signature (délai d'envoi de l'échéancier).
 * Les week-ends et jours fériés ne sont pas décalés.
 */
@Service
public class PremierPrelevementService {

    private static final int DELAI_ECHEANCIER_JOURS = 14;

    public LocalDate calculer(LocalDate dateDebut, int jourPrelevement, LocalDate dateSignature) {
        LocalDate echeancier = dateSignature.plusDays(DELAI_ECHEANCIER_JOURS);
        LocalDate auPlusTot = dateDebut.isAfter(echeancier) ? dateDebut : echeancier;
        LocalDate candidate = auPlusTot.withDayOfMonth(jourPrelevement);
        return candidate.isBefore(auPlusTot) ? candidate.plusMonths(1) : candidate;
    }
}
