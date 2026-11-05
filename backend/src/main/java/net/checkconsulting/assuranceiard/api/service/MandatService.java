package net.checkconsulting.assuranceiard.api.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.MandatPreparationResponse;
import net.checkconsulting.assuranceiard.api.dto.MandatSignatureRequest;
import net.checkconsulting.assuranceiard.api.dto.MandatSigneResponse;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.exception.InformationRefuseeException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.model.MandatSepa;
import net.checkconsulting.assuranceiard.api.repository.DossierSouscriptionRepository;
import net.checkconsulting.assuranceiard.api.repository.MandatSepaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-14 : prépare puis signe le mandat SEPA d'un dossier. Aucun prélèvement réel n'est déclenché. */
@Service
@RequiredArgsConstructor
public class MandatService {

    public static final String CREANCIER = "Assur'Easy";
    /** PROVISOIRE : identifiant créancier SEPA fictif, à remplacer par le vrai ICS d'Assur'Easy. */
    public static final String ICS = "FR00ZZZ000000";

    private static final int DELAI_MAX_JOURS = 90;
    private static final Set<Integer> JOURS_PRELEVEMENT = Set.of(5, 10, 15);
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");
    private static final String ALPHABET_RUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LONGUEUR_ALEA_RUM = 8;
    private static final SecureRandom HASARD = new SecureRandom();

    private final SessionService sessionService;
    private final DossierSouscriptionRepository dossierRepository;
    private final MandatSepaRepository mandatRepository;
    private final IbanService ibanService;
    private final PremierPrelevementService premierPrelevementService;
    private final VerificationBancaire verificationBancaire;
    private final Clock horloge;

    /** Un même dossier garde sa RUM : revenir sur l'étape ne crée pas un autre mandat. */
    @Transactional
    public MandatPreparationResponse preparer(String authorization, UUID referenceDevis) {
        DossierSouscription dossier = dossierDuClient(authorization, referenceDevis);
        return new MandatPreparationResponse(CREANCIER, ICS, mandatDuDossier(dossier).getRum());
    }

    @Transactional
    public MandatSigneResponse signer(String authorization, MandatSignatureRequest demande) {
        DossierSouscription dossier = dossierDuClient(authorization, demande.referenceDevis());
        Instant maintenant = horloge.instant();
        LocalDate aujourdhui = LocalDate.ofInstant(maintenant, FRANCE);
        verifierDateDebut(demande.dateDebut(), aujourdhui);
        verifierJour(demande.jourPrelevement());
        String iban = ibanService.normaliser(demande.iban());
        verifierIban(iban);

        LocalDate premierPrelevement =
                premierPrelevementService.calculer(demande.dateDebut(), demande.jourPrelevement(), aujourdhui);
        MandatSepa mandat = mandatDuDossier(dossier);
        mandat.setTitulaire(demande.titulaire().trim());
        mandat.setIbanFin(ibanService.fin(iban));
        mandat.setJourPrelevement(demande.jourPrelevement());
        mandat.setDateDebut(demande.dateDebut());
        mandat.setDatePremierPrelevement(premierPrelevement);
        mandat.setDateSignature(maintenant);
        return new MandatSigneResponse(mandat.getRum(), premierPrelevement);
    }

    private DossierSouscription dossierDuClient(String authorization, UUID referenceDevis) {
        Compte compte = sessionService.compteConnecte(authorization);
        return dossierRepository.findByCompteAndReferenceDevis(compte, referenceDevis)
                .orElseThrow(DevisInconnuException::new);
    }

    private MandatSepa mandatDuDossier(DossierSouscription dossier) {
        return mandatRepository.findByDossier(dossier).orElseGet(() -> creerMandat(dossier));
    }

    private MandatSepa creerMandat(DossierSouscription dossier) {
        MandatSepa mandat = new MandatSepa();
        mandat.setDossier(dossier);
        mandat.setRum(nouvelleRum());
        mandat.setDateCreation(horloge.instant());
        return mandatRepository.save(mandat);
    }

    // Exemple : AE-2026-K7M2QX9P. Lettres et chiffres ambigus (I, O, 0, 1) exclus.
    private String nouvelleRum() {
        StringBuilder alea = new StringBuilder(LONGUEUR_ALEA_RUM);
        for (int i = 0; i < LONGUEUR_ALEA_RUM; i++) {
            alea.append(ALPHABET_RUM.charAt(HASARD.nextInt(ALPHABET_RUM.length())));
        }
        return "AE-" + LocalDate.ofInstant(horloge.instant(), FRANCE).getYear() + "-" + alea;
    }

    // RG1
    private void verifierDateDebut(LocalDate dateDebut, LocalDate aujourdhui) {
        if (!dateDebut.isAfter(aujourdhui)) {
            throw new InformationRefuseeException("dateDebut", "La date de début doit être au plus tôt demain.");
        }
        if (dateDebut.isAfter(aujourdhui.plusDays(DELAI_MAX_JOURS))) {
            throw new InformationRefuseeException(
                    "dateDebut", "La date de début doit être dans les 90 prochains jours.");
        }
    }

    // RG3
    private void verifierJour(int jourPrelevement) {
        if (!JOURS_PRELEVEMENT.contains(jourPrelevement)) {
            throw new InformationRefuseeException("jourPrelevement", "Choisissez le 5, le 10 ou le 15 du mois.");
        }
    }

    // RG2
    private void verifierIban(String iban) {
        if (!ibanService.valide(iban)) {
            throw new InformationRefuseeException("iban", "Cet IBAN n'est pas valide. Vérifiez sa saisie.");
        }
        if (!verificationBancaire.compteValide(iban)) {
            throw new InformationRefuseeException(
                    "iban", "Ce compte n'a pas pu être vérifié. Indiquez un autre compte.");
        }
    }
}
